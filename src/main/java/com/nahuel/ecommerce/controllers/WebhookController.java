package com.nahuel.ecommerce.controllers;


import com.nahuel.ecommerce.checkout.WebhookService;
import com.nahuel.ecommerce.dtos.NotificacionMercadoPagoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> recibirNotificacion(@RequestHeader("x-signature") String xSignature,
                                                    @RequestHeader("x-request-id") String xRequestId,
                                                    @RequestParam("data.id") String dataId,
                                                    @RequestBody NotificacionMercadoPagoDto notificacion){
        webhookService.process(dataId, notificacion.getTipo(), xSignature, xRequestId);
        return ResponseEntity.ok().build();
    }


}
