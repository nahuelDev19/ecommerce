package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class ErrorProcesamientoArchivoException extends AppException{

    public ErrorProcesamientoArchivoException(String message, Throwable causa) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "FILE_PROCESSING_ERROR", message);
        initCause(causa);
    }
}
