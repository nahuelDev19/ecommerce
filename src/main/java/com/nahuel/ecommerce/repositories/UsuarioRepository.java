package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
}
