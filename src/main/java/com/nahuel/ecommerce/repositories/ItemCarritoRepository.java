package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, UUID> {


    Optional<ItemCarrito> findByCarritoIdAndProductoId(UUID carritoId, UUID productoId);
    List<ItemCarrito> findByCarritoId(UUID carritoId);


}
