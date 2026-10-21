package com.nahuel.ecommerce.controllers;


import com.nahuel.ecommerce.checkout.WebhookService;
import com.nahuel.ecommerce.dtos.NotificacionMercadoPagoDto;
import com.nahuel.ecommerce.exceptions.FirmaWebhookInvalidaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> recibirNotificacion(
            @RequestHeader("x-signature") String xSignature,
            @RequestHeader("x-request-id") String xRequestId,
            @RequestParam("data.id") String dataId,
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


}
