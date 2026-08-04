package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends AppException {

    public RecursoNoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
