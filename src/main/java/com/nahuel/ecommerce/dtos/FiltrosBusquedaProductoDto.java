package com.nahuel.ecommerce.dtos;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiltrosBusquedaProductoDto {

    private String nombre;


    private BigDecimal precioMinimo;

    private BigDecimal precioMaximo;

    private Boolean soloActivos;

    private  Direccion direccion= Direccion.ASC;

    private String sortBy="nombre";

    private int page = 0;

    private int size = 10;

}
