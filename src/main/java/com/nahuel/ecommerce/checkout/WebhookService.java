package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.entitys.EstadoPago;
import com.nahuel.ecommerce.entitys.Orden;
import com.nahuel.ecommerce.entitys.Pago;
import com.nahuel.ecommerce.exceptions.FirmaWebhookInvalidaException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.repositories.OrdenRepository;
import com.nahuel.ecommerce.repositories.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class WebhookService {

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    private final OrdenRepository ordenRepository;
    private final PagoRepository pagoRepository;
    private final PasarelaPago pasarelaPago;


    public boolean validarFirma(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }

        String timestamp = null;
        String firmaRecibida = null;
        for (String parte : xSignature.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length != 2) {
                continue;
            }
            if ("ts".equals(par[0].trim())) {
                if (timestamp != null) {
                    return false;
                }
                timestamp = par[1].trim();
            } else if ("v1".equals(par[0].trim())) {
                if (firmaRecibida != null) {
                    return false;
                }
                firmaRecibida = par[1].trim();
            }
        }

        if (timestamp == null || !timestamp.matches("[0-9]+")
                || firmaRecibida == null || !firmaRecibida.matches("[0-9a-fA-F]{64}")) {
            return false;
        }

        // Formato del mensaje firmado por Mercado Pago; no es el JSON del aviso.
        StringBuilder mensaje = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            mensaje.append("id:").append(dataId.trim()).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            mensaje.append("request-id:").append(xRequestId.trim()).append(';');
        }
        mensaje.append("ts:").append(timestamp).append(';');

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] firmaEsperada = mac.doFinal(mensaje.toString().getBytes(StandardCharsets.UTF_8));
            byte[] firmaRecibidaBytes = HexFormat.of().parseHex(firmaRecibida);
            return MessageDigest.isEqual(firmaEsperada, firmaRecibidaBytes);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", ex);
        }
    }

    @Transactional
    public void process(String dataId, String type, String signature, String requestId){
        //validar firma
        if (!validarFirma(signature, requestId, dataId)) {
            throw new FirmaWebhookInvalidaException();
        }
        //filtrar eventos
        if (!"payment".equalsIgnoreCase(type)) {
            return;
        }
        // La firma siempre se valida primero: omitir el ID no evita el 401.
        // Un aviso firmado sin ID no permite identificar el recurso a consultar.
        if (dataId == null || dataId.isBlank()) {
            log.warn("Webhook firmado sin data.id; no se consulta la pasarela. requestId={}", requestId);
            return;
        }
        // El cuerpo del aviso no acredita el pago: consultamos sus datos reales
        // mediante la pasarela. El adapter resuelve HTTP y traduce la respuesta.
        PagoConsultado payment = pasarelaPago.consultarPago(dataId);
        // verificar referencia de la orden
        String referencia = payment.referenciaOrden();
        if (referencia == null || referencia.isBlank()) {
            throw new ReglaNegocioException(
                    "El pago no tiene referencia de la orden"
            );
        }
        UUID ordenId;
        try {
            ordenId = UUID.fromString(referencia);
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException(
                    "La referencia del pago no es un UUID válido"
            );
        }
        // buscar orden
        Orden orden = ordenRepository.buscarPorIdParaActualizar(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la orden asociada al pago"
                ));
        // El bloqueo pudo hacernos esperar: consultamos otra vez para no
        // guardar un estado obtenido antes de que otro aviso se procesara.
        payment = pasarelaPago.consultarPago(dataId);
        if (!ordenId.toString().equalsIgnoreCase(payment.referenciaOrden())) {
            throw new ReglaNegocioException(
                    "La referencia del pago cambió al bloquear la orden"
            );
        }
        // verificar importe y moneda
        if (payment.monto() == null || orden.getTotal() == null
                || payment.monto().compareTo(orden.getTotal()) != 0) {
            throw new ReglaNegocioException(
                    "El importe del pago no coincide con el total de la orden"
            );
        }
        if (orden.getMoneda() == null) {
            throw new ReglaNegocioException(
                    "La orden no tiene una moneda definida"
            );
        }
        String monedaEsperada = switch (orden.getMoneda()) {
            case ARG -> "ARS";
            case USD -> "USD";
            case EUR -> "EUR";
        };
        if (!monedaEsperada.equals(payment.moneda())) {
            throw new ReglaNegocioException(
                    "La moneda del pago no coincide con la moneda de la orden"
            );
        }
        if (payment.pagoExternoId() == null) {
            throw new IllegalStateException(
                    "La respuesta de la pasarela no tiene identificador de pago"
            );
        }
        if (payment.preferenciaId() == null || payment.preferenciaId().isBlank()) {
            throw new ReglaNegocioException("La pasarela no identificó la preferencia del pago");
        }
        Pago pago = buscarOPrepararPago(orden, payment);

        if (!pago.getOrden().getId().equals(orden.getId())) {
            throw new ReglaNegocioException(
                    "El pago ya está asociado a otra orden"
            );
        }
        if (pago.getPreferenciaId() != null
                && !pago.getPreferenciaId().equals(payment.preferenciaId())) {
            throw new ReglaNegocioException("El pago ya está asociado a otra preferencia");
        }
        if (payment.estado() == null) {
            throw new IllegalStateException(
                    "La pasarela no informó el estado del pago"
            );
        }
        boolean cambioPago = pago.getId() == null
                || !Objects.equals(pago.getPagoExternoId(), payment.pagoExternoId())
                || !Objects.equals(pago.getPreferenciaId(), payment.preferenciaId())
                || pago.getMonto() == null || pago.getMonto().compareTo(payment.monto()) != 0
                || pago.getMoneda() != orden.getMoneda()
                || pago.getEstado() != payment.estado();
        // Un aviso idéntico no modifica ni siquiera actualizadoEn. Un cambio
        // real de estado del mismo pago sí debe persistirse.
        if (cambioPago) {
            pago.setPagoExternoId(payment.pagoExternoId());
            pago.setPreferenciaId(payment.preferenciaId());
            pago.setMonto(payment.monto());
            pago.setMoneda(orden.getMoneda());
            pago.setEstado(payment.estado());
            pago.setActualizadoEn(Instant.now());
            // La violación de UNIQUE se propaga hasta fuera de esta transacción:
            // no consultamos ni reintentamos con una sesión que ya falló.
            pagoRepository.saveAndFlush(pago);
        }
        //evitar reprocesar una orden pagada
        if (orden.getEstado() == EstadoOrden.PAGADA) {
            return;
        }
        // procesar estado
        if (payment.estado() == EstadoPago.APROBADO) {
            approve(orden, payment);
        }
    }

    private Pago buscarOPrepararPago(Orden orden, PagoConsultado payment) {
        // process ya bloqueó la orden: dos avisos para ella no pueden consumir
        // simultáneamente el mismo registro pendiente dentro de este flujo.
        var existente = pagoRepository.findByPagoExternoId(payment.pagoExternoId());
        if (existente.isPresent()) {
            return existente.get();
        }

        var pagosOrden = pagoRepository.findByOrdenId(orden.getId());
        var mismaPreferencia = pagosOrden.stream()
                .filter(p -> payment.preferenciaId().equals(p.getPreferenciaId()))
                .toList();
        var pendientesSinCobro = mismaPreferencia.stream()
                .filter(p -> p.getPagoExternoId() == null && p.getEstado() == EstadoPago.PENDIENTE)
                .toList();
        if (pendientesSinCobro.size() > 1) {
            throw new ReglaNegocioException("Hay más de un pago pendiente para la misma preferencia");
        }
        if (pendientesSinCobro.size() == 1) {
            // Solo completamos el pendiente cuya preferencia confirmó el proveedor.
            return pendientesSinCobro.get(0);
        }
        if (mismaPreferencia.isEmpty()
                && pagosOrden.stream().anyMatch(p -> p.getPreferenciaId() != null)) {
            throw new ReglaNegocioException("La preferencia del pago no pertenece a esta orden");
        }

        // Otro ID externo es otro intento: no sobrescribimos el anterior.
        // Para órdenes antiguas sin preferencia local, conservamos el registro
        // de pagos tras verificar la referencia y la preferencia en la API.
        Pago nuevo = new Pago();
        nuevo.setOrden(orden);
        nuevo.setCreadoEn(Instant.now());
        if (!mismaPreferencia.isEmpty()) {
            nuevo.setUrlPago(mismaPreferencia.get(0).getUrlPago());
        }
        return nuevo;
    }


    private void approve(Orden orden, PagoConsultado payment){
        if (payment.pagoExternoId() == null || payment.fechaAprobacion() == null) {
            throw new IllegalStateException(
                    "El pago aprobado no tiene identificador o fecha de aprobación"
            );
        }

        /*
         * PENDIENTE DE CONSULTA CON EL PROFESOR: política de pagos tardíos (5.4).
         * Propuesta desactivada por decisión del alumno: conservar una orden
         * CANCELADA con un Pago APROBADO exige una forma de atención comercial.
         * Un log no notifica al comercio ni permite seguir la resolución del caso.
         * Falta decidir si se acepta la compra, se gestiona una incidencia o se
         * reembolsa; no se implementaron panel, notificaciones ni devoluciones.
         * Se conserva el código para discutirlo, sin dar este requisito por resuelto.
         * Mientras está comentado, rige el comportamiento anterior: un pago
         * aprobado puede pasar una orden CANCELADA a PAGADA, sin comparar plazos.
         *
        if (orden.getEstado() == EstadoOrden.CANCELADA) {
            // Política para cancelación por vencimiento: importa cuándo se aprobó
            // el cobro, no cuándo llegó el aviso. Solo reabrimos si se pagó ANTES
            // de venceEn; el instante exacto del vencimiento ya queda fuera de plazo.
            // El Pago APROBADO ya se guardó: conservar CANCELADA no devuelve dinero.
            if (orden.getVenceEn() == null) {
                log.warn("Pago aprobado para orden cancelada sin vencimiento; requiere revisión manual. "
                                + "ordenId={}, pagoExternoId={}", orden.getId(), payment.pagoExternoId());
                return;
            }
            if (!payment.fechaAprobacion().isBefore(orden.getVenceEn())) {
                log.warn("Pago aprobado fuera de plazo para orden cancelada; requiere revisión manual. "
                                + "ordenId={}, pagoExternoId={}, aprobadoEn={}, venceEn={}",
                        orden.getId(), payment.pagoExternoId(), payment.fechaAprobacion(), orden.getVenceEn());
                return;
            }
        }
         */

        orden.setEstado(EstadoOrden.PAGADA);
        orden.setPaymentId(payment.pagoExternoId());
        orden.setPagadoEn(payment.fechaAprobacion());
        orden.setActualizadoEn(Instant.now());
        ordenRepository.save(orden);
    }

}
