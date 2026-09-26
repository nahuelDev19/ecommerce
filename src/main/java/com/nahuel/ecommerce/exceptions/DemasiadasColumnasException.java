package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class DemasiadasColumnasException extends AppException {

    private static final String CODE = "TOO_MANY_COLUMNS";
    private static final String MESSAGE =
            "El archivo contiene más columnas de las permitidas";

    public DemasiadasColumnasException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, CODE, MESSAGE);
    }
}
