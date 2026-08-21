package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.UsuarioDto;

import java.util.List;
import java.util.UUID;

public interface UsuarioService {

    UsuarioDto guardar(UsuarioDto dto);

    List<UsuarioDto> listar();

    UsuarioDto buscarPorId(UUID id,UUID autenticadoId);

    void eliminar(UUID id,UUID autenticadoId);

}