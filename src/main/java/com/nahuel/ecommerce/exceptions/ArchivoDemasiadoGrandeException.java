package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class ArchivoDemasiadoGrandeException extends AppException {

    private static final String CODE = "FILE_TOO_LARGE";
    private static final String MESSAGE = "El archivo supera el tamaño máximo permitido";

    public ArchivoDemasiadoGrandeException() {
        super(HttpStatus.CONTENT_TOO_LARGE, CODE, MESSAGE);
    }
}
