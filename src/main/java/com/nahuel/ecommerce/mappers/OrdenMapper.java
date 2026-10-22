package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.OrdenDto;
import com.nahuel.ecommerce.entitys.Orden;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrdenMapper {

    private final ItemOrdenMapper itemOrdenMapper;

    public OrdenDto toDto(Orden orden) {
        return new OrdenDto(
                orden.getId(),
                orden.getEstado(),
                itemOrdenMapper.toDto(orden.getItems()),
                orden.getSubtotal(),
                orden.getDescuentoTotal(),
                orden.getTotal(),
                orden.getMoneda(),
                orden.getCreadoEn(),
                orden.getActualizadoEn(),
                orden.getPagadoEn(),
                orden.getVenceEn()
        );
    }

    public List<OrdenDto> toDto(List<Orden> ordenes) {
        return ordenes.stream().map(this::toDto).toList();
    }
}
