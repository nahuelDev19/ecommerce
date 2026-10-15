package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class DemasiadasFilasException extends AppException {

    private static final String CODE = "TOO_MANY_ROWS";
    private static final String MESSAGE =
            "El archivo contiene más filas de las permitidas";

    public DemasiadasFilasException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, CODE, MESSAGE);
    }
}
