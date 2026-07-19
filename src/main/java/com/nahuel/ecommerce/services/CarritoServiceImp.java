package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.nahuel.ecommerce.services.CuponServiceImp.calcularDescuento;

@RequiredArgsConstructor
@Transactional
@Service
public class CarritoServiceImp implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public CarritoDto guardar(CarritoDto dto) {

        Carrito carrito = toEntity(dto);

        carrito = carritoRepository.save(carrito);

        return toDto(carrito);
    }

    @Override
    public List<CarritoDto> listar() {

        return carritoRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Optional<CarritoDto> buscarPorId(UUID id) {
        return Optional.ofNullable(carritoRepository.findById(id)
                .map(this::toDto).orElseThrow(() -> new RuntimeException("Carrito no encontrado")));
    }

    @Override
    public void eliminar(UUID id) {
        Carrito carrito= carritoRepository.findById(id).orElseThrow(()-> new  RuntimeException("Carrito no encontrado"));
        carritoRepository.delete(carrito);
    }

    private CarritoDto toDto(Carrito carrito) {

        CarritoDto dto = new CarritoDto();

        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());
        dto.setCreadoEn(carrito.getCreadoEn());
        dto.setActualizadoEn(carrito.getActualizadoEn());
        dto.setUltimaInteraccion(carrito.getUltimaInteraccion());
        dto.setSubtotal(calcularTotal(carrito));
        dto.setTotal(calcularDescuento(carrito,calcularTotal(carrito)));
        dto.setItems(
                carrito.getItems()
                        .stream()
                        .map(this::toDto)
                        .toList()
        );

        return dto;
    }


    private Carrito toEntity(CarritoDto dto) {

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Carrito carrito = new Carrito();

        carrito.setId(dto.getId());
        carrito.setUsuario(usuario);

        return carrito;

    }

    private BigDecimal calcularTotal(Carrito carrito) {
        return carrito.getItems().stream()
                .map(item -> item.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
}