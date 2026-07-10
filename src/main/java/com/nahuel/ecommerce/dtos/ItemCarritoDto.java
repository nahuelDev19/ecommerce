package com.nahuel.ecommerce.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ItemCarritoDto {

        private UUID id;
        private UUID productoId;
        private String nombreProducto;
        private BigDecimal precioUnitario;
        private Integer cantidad;
        private BigDecimal subtotal;

}
