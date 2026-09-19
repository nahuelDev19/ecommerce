package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.services.CalculadoraCarrito;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CarritoMapper {

    private final ItemCarritoMapper itemCarritoMapper;
    private final CalculadoraCarrito calculadoraCarrito;

    public CarritoDto toDto(Carrito carrito) {
        Instant ahora= Instant.now();
        List<ItemCarritoDto> items = itemCarritoMapper.toDto(carrito.getItems(),ahora);
        BigDecimal subtotal  = calculadoraCarrito.subtotal(items);
        BigDecimal descuento = calculadoraCarrito.descuento(carrito.getCupon(), subtotal);

        CarritoDto dto = new CarritoDto();
        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());
        dto.setCreadoEn(carrito.getCreadoEn());
        dto.setActualizadoEn(carrito.getActualizadoEn());
        dto.setUltimaInteraccion(carrito.getUltimaInteraccion());
        dto.setItems(items);
        dto.setSubtotal(subtotal);
        dto.setDescuentoTotal(descuento);
        dto.setTotal(subtotal.subtract(descuento));

        return dto;
    }

    public List<CarritoDto> toDto(List<Carrito> carritos) {
        return carritos.stream().map(this::toDto).toList();
    }
}