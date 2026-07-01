package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {
}
