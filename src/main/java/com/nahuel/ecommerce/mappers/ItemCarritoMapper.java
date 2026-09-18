package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Component
public class ItemCarritoMapper {

    public ItemCarritoDto toDto(ItemCarrito item) {
        ItemCarritoDto dto = new ItemCarritoDto();

        dto.setId(item.getId());
        dto.setProductoId(item.getProducto().getId());
        dto.setNombreProducto(item.getProducto().getNombre());
        dto.setPrecioUnitario(item.getPrecioUnitario());   // ← el día del snapshot: item.precioVigente(ahora)
        dto.setCantidad(item.getCantidad());
        dto.setSubtotal(item.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(item.getCantidad())));

        return dto;
    }

    public List<ItemCarritoDto> toDto(Collection<ItemCarrito> items) {
        return items.stream().map(this::toDto).toList();
    }
}