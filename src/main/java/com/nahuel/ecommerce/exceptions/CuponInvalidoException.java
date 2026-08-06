package com.nahuel.ecommerce.exceptions;

public class CuponInvalidoException extends ReglaNegocioException{
    public CuponInvalidoException(String message) {
        super(message);
    }
}
