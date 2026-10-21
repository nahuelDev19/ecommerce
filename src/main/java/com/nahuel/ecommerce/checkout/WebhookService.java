package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.dtos.PaymentResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class WebhookService {

    @Value("${mercadopago.access-token}")
    private String accessToken;
    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;
    @Value("${mercadopago.api-url}")
    private String apiURL;

    private final RestClient.Builder restClientBuilder;
    private final OrdenRepository ordenRepository;
    private final PagoRepository pagoRepository;


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
        // consultar pago en mercado pago
        PaymentResponse payment= getPayment(dataId);
        // verificar referencia de la orden
        String referencia = payment.getReferenciaExterna();
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
        payment = getPayment(dataId);
        if (!ordenId.toString().equalsIgnoreCase(payment.getReferenciaExterna())) {
            throw new ReglaNegocioException(
                    "La referencia del pago cambió al bloquear la orden"
            );
        }
        // verificar importe y moneda
        if (payment.getMonto() == null || orden.getTotal() == null
                || payment.getMonto().compareTo(orden.getTotal()) != 0) {
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
        if (!monedaEsperada.equals(payment.getMoneda())) {
            throw new ReglaNegocioException(
                    "La moneda del pago no coincide con la moneda de la orden"
            );
        }
        if (payment.getId() == null) {
            throw new IllegalStateException(
                    "La respuesta de Mercado Pago no tiene identificador"
            );
        }
        String pagoExternoId = payment.getId().toString();
        Pago pago = pagoRepository.findByPagoExternoId(pagoExternoId)
                .orElseGet(() -> {
                    Pago nuevoPago = new Pago();
                    nuevoPago.setOrden(orden);
                    nuevoPago.setPagoExternoId(pagoExternoId);
                    nuevoPago.setCreadoEn(Instant.now());
                    return nuevoPago;
                });

        if (!pago.getOrden().getId().equals(orden.getId())) {
            throw new ReglaNegocioException(
                    "El pago ya está asociado a otra orden"
            );
        }
        if (payment.getEstado() == null) {
            throw new IllegalStateException(
                    "Mercado Pago no informó el estado del pago"
            );
        }
        EstadoPago estadoPago = switch (payment.getEstado()) {
            case "pending" -> EstadoPago.PENDIENTE;
            case "in_process" -> EstadoPago.EN_PROCESO;
            case "approved" -> EstadoPago.APROBADO;
            case "rejected" -> EstadoPago.RECHAZADO;
            case "cancelled" -> EstadoPago.CANCELADO;
            default -> throw new ReglaNegocioException(
                    "Estado de pago no contemplado: " + payment.getEstado()
            );
        };
        pago.setMonto(payment.getMonto());
        pago.setMoneda(orden.getMoneda());
        pago.setEstado(estadoPago);
        pago.setActualizadoEn(Instant.now());
        pagoRepository.save(pago);
        //evitar reprocesar una orden pagada
        if (orden.getEstado() == EstadoOrden.PAGADA) {
            return;
        }
        // procesar estado
        if ("approved".equals(payment.getEstado())) {
            approve(orden, payment);
        }
    }


    private void approve(Orden orden, PaymentResponse payment){
        if (payment.getId() == null || payment.getFechaAprobacion() == null) {
            throw new IllegalStateException(
                    "El pago aprobado no tiene identificador o fecha de aprobación"
            );
        }

        orden.setEstado(EstadoOrden.PAGADA);
        orden.setPaymentId(payment.getId().toString());
        orden.setPagadoEn(payment.getFechaAprobacion().toInstant());
        orden.setActualizadoEn(Instant.now());
        ordenRepository.save(orden);
    }

    private PaymentResponse getPayment(String paymentId) {
        // El aviso identifica el pago; consultamos la API para obtener
        // su estado, importe y moneda, sin confiar en el cuerpo del aviso.
        PaymentResponse payment = restClientBuilder.build()
                .get()
                .uri(apiURL + "/v1/payments/{id}", paymentId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(PaymentResponse.class);

        if (payment == null) {
            throw new IllegalStateException(
                    "Mercado Pago devolvió una respuesta sin datos del pago"
            );
        }

        return payment;
    }
}
