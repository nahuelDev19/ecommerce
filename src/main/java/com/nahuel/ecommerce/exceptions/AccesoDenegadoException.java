package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends AppException{
    public AccesoDenegadoException() {
        super(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "No tiene permisos para realizar esta acción"
        );
    }
}
