package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CarritoRepository extends JpaRepository<Carrito, UUID> {

    Optional<Carrito> findByUsuarioIdAndEstadoCarrito(UUID usuarioId, EstadoCarrito estado);


}
