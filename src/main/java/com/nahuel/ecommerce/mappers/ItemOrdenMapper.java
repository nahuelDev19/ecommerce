package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.ItemOrdenDto;
import com.nahuel.ecommerce.entitys.ItemOrden;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class ItemOrdenMapper {

    public ItemOrdenDto toDto(ItemOrden item) {
        return new ItemOrdenDto(
                item.getId(),
                item.getProductoId(),
                item.getNombreProducto(),
                item.getPrecioUnitario(),
                item.getCantidad(),
                item.getSubtotal()
        );
    }

    public List<ItemOrdenDto> toDto(Collection<ItemOrden> items) {
        return items.stream().map(this::toDto).toList();
    }
}
