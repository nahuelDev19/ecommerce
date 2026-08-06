package com.nahuel.ecommerce.exceptions;

public class FormulaNoPermitidaException extends ReglaNegocioException{
    public FormulaNoPermitidaException() {
        super("No se permiten fórmulas en el archivo Excel");
    }
}
