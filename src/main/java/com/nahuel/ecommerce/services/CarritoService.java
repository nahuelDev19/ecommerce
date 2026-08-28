package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoService {

    CarritoDto guardar(CarritoDto dto, UUID idUsuario);

    List<CarritoDto> listar(UUID idUsuario);

    CarritoDto buscarPorId(UUID id,UUID idUsuario);

    void eliminar(UUID id,UUID idUsuario);
    CarritoDto buscarCarritoActivoDelUsuarioAutenticado(UUID idUsuario);

}
