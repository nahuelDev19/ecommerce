package com.nahuel.ecommerce.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemCarritoDto {



    private UUID productoId;
    private Integer cantidad;
    private ProductoDto productoDto;
    private BigDecimal precioUnitario;
    private BigDecimal subTotal;

}
