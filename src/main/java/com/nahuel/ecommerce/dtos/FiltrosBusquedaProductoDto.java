package com.nahuel.ecommerce.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltrosBusquedaProductoDto {

    private String consulta;

    private List<UUID> idsCategorias;

    private BigDecimal precioMinimo;

    private BigDecimal precioMaximo;

    private Boolean soloActivos;

}
