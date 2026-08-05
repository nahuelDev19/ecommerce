package com.nahuel.ecommerce.exceptions;

public class CuponYaAplicadoException extends ReglaNegocioException{
    public CuponYaAplicadoException() {
        super("El carrito ya tiene un cupón aplicado");
    }
}
