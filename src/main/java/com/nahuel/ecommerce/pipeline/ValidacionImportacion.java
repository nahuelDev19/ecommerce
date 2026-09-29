package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.Producto;

import java.util.List;

public record ValidacionImportacion(
        List<Producto> productosValidos,
        ResultadoImportacionDto reporte
) {
    public boolean tieneErrores() {
        return reporte.getLineasDescartadasPorFormatoInvalido() > 0
                || !reporte.getErrores().isEmpty();
    }
}