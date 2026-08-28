package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.nahuel.ecommerce.services.CuponServiceImp.calcularDescuento;

@RequiredArgsConstructor
@Transactional
@Service
public class CarritoServiceImp implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public CarritoDto guardar(CarritoDto dto, UUID idUsuario) {

        // Se ignora deliberadamente dto.getId() y dto.getUsuarioId():
        // el id lo genera la BD y el dueño SIEMPRE es el usuario autenticado.
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);

        carrito = carritoRepository.save(carrito);
        return toDto(carrito);
    }

    @Override
    public List<CarritoDto> listar(UUID idUsuario) {

        return carritoRepository.findByUsuarioId(idUsuario)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public CarritoDto buscarPorId(UUID id,UUID idUsuario) {
        return carritoRepository.findByIdAndUsuarioId(id, idUsuario)
                .map(this::toDto).orElseThrow(() -> new RecursoNoEncontradoException("Carrito no encontrado"));
    }

    @Override
    public void eliminar(UUID id,UUID idUsuario) {
        Carrito carrito= carritoRepository.findByIdAndUsuarioId(id,idUsuario)
                .orElseThrow(()-> new RecursoNoEncontradoException("Carrito no encontrado"));
        carritoRepository.delete(carrito);
    }

    @Override
    public CarritoDto buscarCarritoActivoDelUsuarioAutenticado(UUID idUsuario) {
        Carrito carrito= carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO).orElseThrow(()-> new RecursoNoEncontradoException("no cuenta con un carrito activo"));
        return  toDto(carrito);
    }

    private CarritoDto toDto(Carrito carrito) {

        BigDecimal montoDescuento= calcularDescuento(carrito,calcularTotal(carrito));
        BigDecimal total= calcularTotal(carrito);
        BigDecimal totalConDescuento=total.subtract(montoDescuento);
        CarritoDto dto = new CarritoDto();

        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());
        dto.setCreadoEn(carrito.getCreadoEn());
        dto.setActualizadoEn(carrito.getActualizadoEn());
        dto.setUltimaInteraccion(carrito.getUltimaInteraccion());
        dto.setSubtotal(total);
        dto.setDescuentoTotal(montoDescuento);
        dto.setTotal(totalConDescuento);
        dto.setItems(
                carrito.getItems()
                        .stream()
                        .map(this::toDto)
                        .toList()
        );

        return dto;
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