package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
public class ArchivoComprimidoPeligrosoException extends AppException {

    public ArchivoComprimidoPeligrosoException(Throwable cause) {
        super(
                HttpStatus.BAD_REQUEST,
                "ARCHIVO_COMPRIMIDO_PELIGROSO",
                "El archivo contiene una expansión comprimida sospechosa",
                cause
        );
    }
}

