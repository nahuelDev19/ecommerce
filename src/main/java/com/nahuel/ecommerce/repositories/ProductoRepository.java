package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Producto;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {


    @Query("select p from Producto p where p.activo=false")
    List<Producto> findAllActivos();
}
