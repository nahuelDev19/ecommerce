package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Cupon;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CalculadoraCarrito {

    /** Suma los subtotales YA resueltos por el mapper (no recalcula precios). */
    public BigDecimal subtotal(List<ItemCarritoDto> items) {
        return items.stream()
                .map(ItemCarritoDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal descuento(Cupon cupon, BigDecimal subtotal) {
        if (cupon == null) {
            return BigDecimal.ZERO;
        }
        if (cupon.getMontoMinimoCarrito() != null
                && subtotal.compareTo(cupon.getMontoMinimoCarrito()) < 0) {
            return BigDecimal.ZERO;
        }

        return switch (cupon.getTipoDescuento()) {
            case PORCENTAJE -> subtotal
                    .multiply(cupon.getValorDescuento())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case MONTOFIJO -> cupon.getValorDescuento().min(subtotal);
            default -> BigDecimal.ZERO;
        };
    }
}