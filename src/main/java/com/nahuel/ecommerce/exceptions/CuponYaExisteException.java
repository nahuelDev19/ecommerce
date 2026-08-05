package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class CuponYaExisteException extends AppException{
    public CuponYaExisteException(String codigo) {
        super(HttpStatus.CONFLICT, "DUPLICATE_COUPON", "Ya existe un cupón con el código: " + codigo);
    }
}
