package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class ProductoServiceImp implements ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoDto guardarProducto(ProductoDto dto) {
        Producto producto = Producto.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precioBase(dto.getPrecioBase())
                .moneda(dto.getMoneda())
                .activo(dto.getActivo())
                .fechaDescontinuado(null)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(null)
                .build();

        Producto guardado = productoRepository.save(producto);

        return ProductoDto.builder()
                .id(guardado.getId())
                .nombre(guardado.getNombre())
                .descripcion(guardado.getDescripcion())
                .precioBase(guardado.getPrecioBase())
                .moneda(guardado.getMoneda())
                .activo(guardado.getActivo())
                .fechaDescontinuado(guardado.getFechaDescontinuado())
                .build();
    }




}
