package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

/** ARCHIVO NUEVO. Ya estaba nombrada en un comentario de SupabaseTokenVerifier, pero no existia. */
public class TokenInvalidoException extends AppException {
    public TokenInvalidoException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", message);
    }
}
