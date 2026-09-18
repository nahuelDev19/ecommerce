package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.entitys.Producto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductoMapper {

    public ProductoDto toDto(Producto producto) {
        return ProductoDto.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precioBase(producto.getPrecioBase())
                .moneda(producto.getMoneda())
                .activo(producto.getActivo())
                .fechaDescontinuado(producto.getFechaDescontinuado())
                .build();
    }

    public List<ProductoDto> toDto(List<Producto> productos) {
        return productos.stream().map(this::toDto).toList();
    }

    /** Solo campos del request: fechaCreacion la sigue poniendo el servicio. */
    public Producto toEntity(ProductoDto dto) {
        return Producto.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precioBase(dto.getPrecioBase())
                .moneda(dto.getMoneda())
                .activo(dto.getActivo())
                .build();
    }
}