package com.nahuel.ecommerce.exceptions;

public class ArchivoVacioException extends ReglaNegocioException{
    public ArchivoVacioException() {
        super("El archivo no puede estar vacío");
    }
}
