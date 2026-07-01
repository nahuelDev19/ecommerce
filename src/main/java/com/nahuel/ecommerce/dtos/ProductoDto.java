package com.nahuel.ecommerce.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDto {

    private UUID id;

    private String nombre;

    private String descripcion;

    private BigDecimal precioBase;

    private String moneda;

    private Boolean activo;

    private Instant fechaDescontinuado;

}
