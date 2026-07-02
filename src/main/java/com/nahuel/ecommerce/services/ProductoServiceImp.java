package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    @Override
    public void eliminarPorId(UUID id) {

    }

    @Override
    public ProductoDto actualizarPorId(UUID id, ProductoDto dto) {
        return null;
    }

    @Override
    public List<ProductoDto> cargarProductosDesdeExcel(InputStream excelStream) {
        return List.of();
    }

    @Override
    public Page<ProductoDto> buscarProductos(Pageable pageable, String nombre, String moneda, Boolean activo) {
        return null;
    }

    @Override
    public void descontinuarProducto(UUID id) {
        Producto productodescontinuado= productoRepository.findById(id).orElseThrow(()-> new RuntimeException("producto no encontrado"));
        productodescontinuado.setFechaDescontinuado(Instant.now());
        productodescontinuado.setActivo(false);
        productoRepository.save(productodescontinuado);
    }

    @Override
    public List<ProductoDto> listarTodos() {
        return List.of();
    }

    @Override
    public List<ProductoDto> listarActivos() {
        return productoRepository.findAllActivos().stream().map(this::convertirProductoDTO).toList();
    }

    @Override
    public List<ProductoDto> listarDescontinuados() {
        return List.of();
    }

    private ProductoDto convertirProductoDTO(Producto producto){
        return new ProductoDto(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecioBase(),
                producto.getMoneda(),
                producto.getActivo(),
                producto.getFechaDescontinuado()
        );
    }

}
