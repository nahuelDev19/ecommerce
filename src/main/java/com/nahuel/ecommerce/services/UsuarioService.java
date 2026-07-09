package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioService {

    Usuario guardar(Usuario usuario);

    List<Usuario> listar();

    Optional<Usuario> buscarPorId(UUID id);

    void eliminar(UUID id);

}
