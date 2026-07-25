package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.FiltrosBusquedaProductoDto;
import com.nahuel.ecommerce.dtos.ProductoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductoService {

    ProductoDto guardarProducto(ProductoDto dto);
    void eliminarPorId(UUID id);

    ProductoDto actualizarPorId(UUID id, ProductoDto dto);


    // Búsqueda paginada con filtros
    Page<ProductoDto> buscarProductos(FiltrosBusquedaProductoDto dto);
    // Eliminación lógica: descontinuar (fechaDescontinuado)
    void descontinuarProducto(UUID id);

    // Listados
    List<ProductoDto> listarTodos();
    List<ProductoDto> listarDesactivos();
    List<ProductoDto> listarDescontinuados();
}
