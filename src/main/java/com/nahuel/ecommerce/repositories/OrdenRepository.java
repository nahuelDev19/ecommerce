package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Orden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<Orden, UUID> {

    Optional<Orden> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
