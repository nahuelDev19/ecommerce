package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.ImportacionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImportacionJobRepository extends JpaRepository<ImportacionJob, UUID> {

    Optional<ImportacionJob> findFirstByHuellaContenidoOrderByFechaCreacionDesc(String huella);
}
