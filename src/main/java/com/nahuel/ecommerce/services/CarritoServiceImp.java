package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.mappers.CarritoMapper;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Transactional
@Service
public class CarritoServiceImp implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CarritoMapper carritoMapper;

    @Override
    public CarritoDto guardar(CarritoDto dto, UUID idUsuario) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO).isPresent()) {
            throw new ReglaNegocioException("El usuario ya tiene un carrito activo.");
        }

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);

        carrito = carritoRepository.save(carrito);
        return carritoMapper.toDto(carrito);
    }

    @Override
    public List<CarritoDto> listar(UUID idUsuario) {

        return carritoRepository.findByUsuarioId(idUsuario)
                .stream()
                .map(carritoMapper::toDto)
                .toList();
    }

    @Override
    public CarritoDto buscarPorId(UUID id,UUID idUsuario) {
        return carritoRepository.findByIdAndUsuarioId(id, idUsuario)
                .map(carritoMapper::toDto).orElseThrow(() -> new RecursoNoEncontradoException("Carrito no encontrado"));
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
        return  carritoMapper.toDto(carrito);
    }



}