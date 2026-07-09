package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        return carritoRepository.findById(id)
                .map(this::toDto);
    }

    @Override
    public void eliminar(UUID id) {

        carritoRepository.deleteById(id);
    }

    private CarritoDto toDto(Carrito carrito) {

        CarritoDto dto = new CarritoDto();

        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());

        return dto;
    }

    private Carrito toEntity(CarritoDto dto) {

        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Carrito carrito = new Carrito();

        carrito.setId(dto.getId());
        carrito.setEstadoCarrito(dto.getEstadoCarrito());
        carrito.setUsuario(usuario);

        return carrito;
    }
}