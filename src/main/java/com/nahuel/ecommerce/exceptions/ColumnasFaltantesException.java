package com.nahuel.ecommerce.exceptions;

import java.util.List;

public class ColumnasFaltantesException extends ReglaNegocioException{
    public ColumnasFaltantesException(List<String> columnas) {
        super("Faltan columnas obligatorias en el archivo: " + columnas);
    }
}
