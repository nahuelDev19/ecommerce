package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class FirmaWebhookInvalidaException extends AppException {

    public FirmaWebhookInvalidaException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "INVALID_WEBHOOK_SIGNATURE",
                "La firma del webhook no es válida"
        );
    }
}
