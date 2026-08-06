package com.nahuel.ecommerce.exceptions;

public class FormatoInvalidoException extends ReglaNegocioException{
    public FormatoInvalidoException(String nombreArchivo) {
        super("Formato de archivo no válido: " + nombreArchivo + ". Formatos permitidos: .xls, .xlsx");
    }
}
