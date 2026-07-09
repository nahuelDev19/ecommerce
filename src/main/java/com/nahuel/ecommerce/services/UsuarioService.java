package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.UsuarioDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioService {

    UsuarioDto guardar(UsuarioDto dto);

    List<UsuarioDto> listar();

    Optional<UsuarioDto> buscarPorId(UUID id);

    void eliminar(UUID id);

}