package com.nahuel.ecommerce.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltrosBusquedaProductoDto {

    private String nombre;

    @DecimalMin(value = "0.0", message = "El precio mínimo no puede ser negativo")
    private BigDecimal precioMinimo;

    @DecimalMin(value = "0.0", message = "El precio máximo no puede ser negativo")
    private BigDecimal precioMaximo;

    private Boolean soloActivos = true;

    private Direccion direccion = Direccion.ASC;

    private String sortBy = "nombre";

    @Min(value = 0, message = "La página no puede ser negativa")
    private int page = 0;

    @Positive(message = "El tamaño de página debe ser mayor a 0")
    @Max(value = 100, message = "El tamaño máximo permitido es 100")
    private int size = 10;

}
