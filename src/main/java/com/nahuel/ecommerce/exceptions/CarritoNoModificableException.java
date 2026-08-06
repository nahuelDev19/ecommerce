package com.nahuel.ecommerce.exceptions;

public class CarritoNoModificableException extends ReglaNegocioException {
    public CarritoNoModificableException(String message) {
        super(message);
    }
}
