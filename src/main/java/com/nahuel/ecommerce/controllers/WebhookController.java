package com.nahuel.ecommerce.controllers;


import com.nahuel.ecommerce.checkout.WebhookService;
import com.nahuel.ecommerce.dtos.NotificacionMercadoPagoDto;
import com.nahuel.ecommerce.exceptions.FirmaWebhookInvalidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/mercadopago")
    // Sin firma válida: 401. Sin request-id: se valida con los datos disponibles.
    // Sin data.id y con firma válida: 200 sin consultar el pago (se registra).
    // JSON ausente o mal formado conserva el 400 de Spring antes de este método.
    public ResponseEntity<Void> recibirNotificacion(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody NotificacionMercadoPagoDto notificacion
    ) {
        try {
            webhookService.process(
                    dataId,
                    notificacion.getTipo(),
                    xSignature,
                    xRequestId
            );
        } catch (FirmaWebhookInvalidaException ex) {
            // Una firma inválida mantiene la respuesta 401 del handler global.
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            // El proxy transaccional ya hizo rollback antes de llegar al controller.
            // Con JPA no dependemos de recibir DuplicateKeyException: inspeccionamos
            // la restricción concreta para no confundir otros errores con duplicados.
            if (esPagoExternoDuplicado(ex)) {
                log.warn("Colisión de ID de pago externo; transacción revertida. pagoId={}, requestId={}",
                        dataId, xRequestId, ex);
            } else {
                log.error("Fallo de integridad al procesar webhook; transacción revertida. pagoId={}, requestId={}",
                        dataId, xRequestId, ex);
            }
        } catch (Exception ex) {
            log.error(
                    "No se pudo procesar el webhook de Mercado Pago. "
                            + "pagoId={}, requestId={}",
                    dataId,
                    xRequestId,
                    ex
            );
        }

        /*
         * Por requisito del punto 21, respondemos 200 ante avisos repetidos
         * y fallos internos durante el procesamiento.
         *
         * Si respondemos con error, Mercado Pago puede reenviar el aviso.
         * Al responder 200 incluso después de un fallo, confirmamos su recepción
         * y ese fallo no provoca un reintento. Por ejemplo, el cliente podría
         * haber pagado y la orden seguir PENDIENTE_PAGO en nuestra base.
         *
         * Registramos el error para investigarlo. Queda pendiente implementar
         * un mecanismo de recuperación: este 200 no garantiza que el pago
         * se haya procesado correctamente.
         */
        return ResponseEntity.ok().build();
    }

    private boolean esPagoExternoDuplicado(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException restriccion
                    && "uq_pago_externo_id".equals(restriccion.getConstraintName())) {
                return true;
            }
        }
        return false;
    }


}
