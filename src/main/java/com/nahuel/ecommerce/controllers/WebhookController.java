package com.nahuel.ecommerce.controllers;


import com.nahuel.ecommerce.checkout.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
                                                    @RequestParam("data.id") String dataId){
        if (!webhookService.validarFirma(xSignature, xRequestId, dataId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    /*
    , asociado a la cabecera x-signature.
xRequestId, asociado a la cabecera xRequestId.
dataId, asociado al parámetro de URL data.id.
     */
}
