package com.nahuel.ecommerce.exceptions;

public class EncabezadoFaltanteException extends ReglaNegocioException{
    public EncabezadoFaltanteException() {
        super("El documento debe contener una fila de encabezados");
    }
}
