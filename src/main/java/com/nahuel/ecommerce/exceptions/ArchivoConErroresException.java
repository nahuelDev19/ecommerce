package com.nahuel.ecommerce.exceptions;

import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import lombok.Data;
import lombok.Getter;

@Getter
public class ArchivoConErroresException extends ReglaNegocioException{
    private final ResultadoImportacionDto reporte;

    public ArchivoConErroresException(String message,ResultadoImportacionDto reporte) {
        super(message);
        this.reporte= reporte;
    }
}
