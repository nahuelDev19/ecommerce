package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class CeldaDemasiadoLargaException extends AppException {

        private static final String CODE = "CELL_TOO_LONG";

        public CeldaDemasiadoLargaException(int fila, int columna, int largo) {
            super(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    CODE,
                    "La celda de la fila " + fila
                            + " y columna " + columna
                            + " supera el largo máximo permitido. Largo actual: "
                            + largo
            );
        }
    }