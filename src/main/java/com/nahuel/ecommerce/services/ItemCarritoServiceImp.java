package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static com.nahuel.ecommerce.entitys.EstadoCarrito.*;

@Transactional
@RequiredArgsConstructor
@Service
public class ItemCarritoServiceImp implements ItemCarritoService{

    private final ItemCarritoRepository itemCarritoRepository;
    private final ProductoRepository productoRepository;
    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoService carritoService;



    public CarritoDto agregarItem(ItemRequestDto dto){

        Usuario usuario= usuarioRepository.findById(dto.getUsuarioId()).orElseThrow(()-> new RuntimeException("usuario no encontrado"));
        Producto producto = productoRepository.findById(dto.getProductoId()).orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Carrito carrito= carritoRepository.findByUsuarioIdAndEstadoCarrito(dto.getUsuarioId(),ACTIVO)
                .orElseGet(()-> {
                    Carrito carrito1 = new Carrito();
                    carrito1.setUsuario(usuario);
                    carrito1.setEstadoCarrito(ACTIVO);
                    carrito1.setCreadoEn(Instant.now());
                    return carritoRepository.save(carrito1);
                });
        Optional<ItemCarrito> itemExistente =itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId());


        if (itemExistente.isPresent()) {

            ItemCarrito item = itemExistente.get();
            item.setCantidad(item.getCantidad() + dto.getCantidad());
            itemCarritoRepository.save(item);

        } else {

            ItemCarrito item = new ItemCarrito();
            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(dto.getCantidad());
            item.setPrecioUnitario(producto.getPrecioBase());

            carrito.getItems().add(item);
            itemCarritoRepository.save(item);
        }

        carrito.setActualizadoEn(Instant.now());
        carrito.setUltimaInteraccion(Instant.now());
        return toDto(carrito);
    }

@Override
public CarritoDto disminuirItem(ItemRequestDto dto) {

    if (dto.getCantidad() <= 0) {
        throw new RuntimeException("La cantidad debe ser mayor a cero");
    }

    Carrito carrito = carritoRepository
            .findByUsuarioIdAndEstadoCarrito(dto.getUsuarioId(), ACTIVO)
            .orElseThrow(() -> new RuntimeException("Carrito no encontrado o inactivo"));

    ItemCarrito item = itemCarritoRepository
            .findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId())
            .orElseThrow(() -> new RuntimeException("El producto no existe en el carrito"));

    int nuevaCantidad = item.getCantidad() - dto.getCantidad();

    if (nuevaCantidad <= 0) {
        carrito.getItems().remove(item);
        itemCarritoRepository.delete(item);
    } else {
        item.setCantidad(nuevaCantidad);
    }

    if (carrito.getItems().isEmpty()) {
            carrito.setEstadoCarrito(VACIO);
    }

    Instant ahora = Instant.now();
    carrito.setActualizadoEn(ahora);
    carrito.setUltimaInteraccion(ahora);

    carritoRepository.save(carrito);

    return toDto(carrito);
}

    @Override
    public CarritoDto eliminarItem(EliminarItemCarritoDto dto) {

        Carrito carrito = carritoRepository
                .findByUsuarioIdAndEstadoCarrito(dto.getUsuarioId(), ACTIVO)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado o inactivo"));

        ItemCarrito item = itemCarritoRepository
                .findByCarritoIdAndProductoId(carrito.getId(), dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("El producto no existe en el carrito"));

        carrito.getItems().remove(item);
        itemCarritoRepository.delete(item);

        if (carrito.getItems().isEmpty()) {
            carrito.setEstadoCarrito(VACIO);
        }

        Instant ahora = Instant.now();
        carrito.setActualizadoEn(ahora);
        carrito.setUltimaInteraccion(ahora);

        carritoRepository.save(carrito);

        return toDto(carrito);
    }
    private CarritoDto toDto(Carrito carrito) {

        CarritoDto dto = new CarritoDto();

        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());
        dto.setCreadoEn(carrito.getCreadoEn());
        dto.setActualizadoEn(carrito.getActualizadoEn());
        dto.setUltimaInteraccion(carrito.getUltimaInteraccion());
        dto.setTotal(calcularTotal(carrito));
        dto.setSubtotal(calcularTotal(carrito));

        dto.setItems(
                carrito.getItems()
                        .stream()
                        .map(this::toDto)
                        .toList()
        );

        return dto;
    }

    private ItemCarritoDto toDto(ItemCarrito item) {

        ItemCarritoDto dto = new ItemCarritoDto();

        dto.setId(item.getId());
        dto.setProductoId(item.getProducto().getId());
        dto.setNombreProducto(item.getProducto().getNombre());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setCantidad(item.getCantidad());
        dto.setSubtotal(item.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(item.getCantidad())));

        return dto;
    }

    private BigDecimal calcularTotal(Carrito carrito) {
        return carrito.getItems().stream()
                .map(item -> item.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
