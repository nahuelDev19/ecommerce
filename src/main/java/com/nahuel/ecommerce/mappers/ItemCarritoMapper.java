package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
public class ItemCarritoMapper {

    public ItemCarritoDto toDto(ItemCarrito item, Instant ahora) {

        BigDecimal precio= item.precioVigente(ahora);
        ItemCarritoDto dto = new ItemCarritoDto();

        dto.setId(item.getId());
        dto.setProductoId(item.getProducto().getId());
        dto.setNombreProducto(item.getProducto().getNombre());
        dto.setPrecioUnitario(precio);
        dto.setCantidad(item.getCantidad());
        dto.setSubtotal(precio
                .multiply(BigDecimal.valueOf(item.getCantidad())));

        return dto;
    }

    public List<ItemCarritoDto> toDto(Collection<ItemCarrito> items,Instant ahora) {
        return items.stream().map(item -> toDto(item,ahora)).toList();
    }
}