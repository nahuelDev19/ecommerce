package com.nahuel.ecommerce.exceptions;

public class CuponExpiradoException extends ReglaNegocioException{
    public CuponExpiradoException(String codigo) {
        super("El cupon "+codigo+" ha expirado");
    }
}
