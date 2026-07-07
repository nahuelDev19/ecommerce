package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CuponesRepository extends JpaRepository<Cupon, UUID> {



    void deleteByActivoFalse();
}
