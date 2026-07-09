package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoService {

    CarritoDto guardar(CarritoDto dto);

    List<CarritoDto> listar();

    Optional<CarritoDto> buscarPorId(UUID id);

    void eliminar(UUID id);
}
