package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.EstadoPago;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoConsultado(
        String pagoExternoId,
        EstadoPago estado,
        BigDecimal monto,
        String moneda,
        String referenciaOrden,
        Instant fechaAprobacion,
        String preferenciaId
) {
}
