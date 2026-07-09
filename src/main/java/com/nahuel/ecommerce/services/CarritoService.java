package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Carrito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoService {

    Carrito guardar(UUID usuarioId);

    List<Carrito> listar();

    Optional<Carrito> buscarPorId(UUID id);

    void eliminar(UUID id);

}
