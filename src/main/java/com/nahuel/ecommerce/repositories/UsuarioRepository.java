package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);
    //Optional<Usuario> findByIdAndUsuarioId(UUID id);
    Optional<Usuario> findBySupabaseId(UUID supabaseId);
    boolean existsByEmail(String email);
    Long countByRol(Rol rol);


}
