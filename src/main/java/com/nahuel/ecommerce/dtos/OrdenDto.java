package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.entitys.TipoMoneda;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrdenDto(
        UUID id,
        EstadoOrden estado,
        List<ItemOrdenDto> items,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal total,
        TipoMoneda moneda,
        Instant creadoEn,
        Instant actualizadoEn,
        Instant pagadoEn,
        Instant venceEn
) {
}
