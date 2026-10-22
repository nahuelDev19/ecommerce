package com.nahuel.ecommerce.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemOrdenDto(
        UUID id,
        UUID productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        Integer cantidad,
        BigDecimal subtotal
) {
}
