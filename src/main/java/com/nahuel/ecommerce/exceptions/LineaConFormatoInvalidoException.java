package com.nahuel.ecommerce.exceptions;

import lombok.Getter;

import java.util.List;

@Getter
public class LineaConFormatoInvalidoException extends ReglaNegocioException {

    private final int numeroFila;

    public LineaConFormatoInvalidoException(int numeroFila, String message) {
        super("Fila " + numeroFila + ": " + message);
        this.numeroFila = numeroFila;
    }

    public LineaConFormatoInvalidoException(int numeroFila, List<String> errores) {
        this(numeroFila, String.join(", ", errores));
    }
}
