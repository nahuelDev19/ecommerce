package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends AppException{


    public ReglaNegocioException(String message) {
        super(HttpStatus.BAD_REQUEST, "BUSINESS_RULE", message);
    }
}
