package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.entitys.EstadoCupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuponesRepository extends JpaRepository<Cupon, UUID> {



    List<Cupon> findByEstadoCupon(EstadoCupon estadoCupon);
    Optional<Cupon> findByCodigoCupon(String codigo);
}
