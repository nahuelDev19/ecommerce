package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.Direccion;
import com.nahuel.ecommerce.dtos.FiltrosBusquedaProductoDto;
import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
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

        return convertirProductoDTO(guardado);
    }

    @Override
    public void eliminarPorId(UUID id) {
        //para uso de cron
        productoRepository.deleteById(id);
    }

    @Override
    public ProductoDto actualizarPorId(UUID id, ProductoDto dto) {
        Producto productoExistente= productoRepository.findById(id).orElseThrow(()-> new RuntimeException("producto no se encontro en DB"));
        productoExistente.setNombre(dto.getNombre());
        productoExistente.setDescripcion(dto.getDescripcion());
        productoExistente.setPrecioBase(dto.getPrecioBase());
        productoExistente.setMoneda(dto.getMoneda());
        productoExistente.setActivo(dto.getActivo());
        productoExistente.setFechaDescontinuado(dto.getFechaDescontinuado());

        Producto actualizado = productoRepository.save(productoExistente);
        return convertirProductoDTO(actualizado);
    }



    @Override
    public Page<ProductoDto> buscarProductos(FiltrosBusquedaProductoDto dto) {

        if(dto.getPrecioMinimo() != null && dto.getPrecioMaximo() != null && dto.getPrecioMinimo().compareTo(dto.getPrecioMaximo())>0){
            throw new RuntimeException("El precio mínimo no puede ser mayor al precio maximo");
        }

        Sort sort = dto.getDireccion()== Direccion.DESC ?
                Sort.by(dto.getSortBy()).descending() :
                Sort.by(dto.getSortBy()).ascending();

        Pageable pageable = PageRequest.of(dto.getPage(),dto.getSize(),sort);

        Page <Producto> resultado = productoRepository.buscarConFiltros(
                dto.getNombre(), dto.getPrecioMinimo(), dto.getPrecioMaximo(),dto.getSoloActivos(), pageable);

        return resultado.map(this::convertirProductoDTO);
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
        List<Producto> listDto= productoRepository.findAll();
        return listDto.stream().map(this::convertirProductoDTO).toList();
    }

    @Override
    public List<ProductoDto> listarDesactivos() {
        return productoRepository.findAllDesactivos().stream().map(this::convertirProductoDTO).toList();
    }

    @Override
    public List<ProductoDto> listarDescontinuados() {
        List<Producto> listDto= productoRepository.findAll();
        return listDto.stream().map(this::convertirProductoDTO).filter(p-> !p.getActivo()).toList();
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
