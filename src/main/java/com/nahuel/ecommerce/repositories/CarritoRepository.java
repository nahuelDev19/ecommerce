package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarritoRepository extends JpaRepository<Carrito, UUID> {

    Optional<Carrito> findByUsuarioIdAndEstadoCarrito(UUID usuarioId, EstadoCarrito estado);

    @Query("""
            SELECT c FROM Carrito c WHERE c.ultimaInteraccion <= :ultimaInteraccion AND
            c.estadoCarrito = 'ACTIVO'
            """)
    List<Carrito> findCarritoUltimaInteraccion(
            @Param("ultimaInteraccion") Instant ultimaInteraccion
    );

    @Query("""
            SELECT c FROM Carrito c WHERE c.estadoCarrito=:estadoCarrito AND 
            c.ultimaInteraccion <= :ultimaInteraccion
            """)
    List<Carrito> findCarritoEstadoCarritoAbandonado(
            @Param("estadoCarrito") EstadoCarrito estadoCarrito,
            @Param("ultimaInteraccion") Instant ultimaInteraccion
    );

    @Query("""
    SELECT c
    FROM Carrito c
    WHERE c.estadoCarrito = 'ACTIVO'
      AND c.ultimaInteraccion <= :limite
""")
    List<Carrito> findCarritosActivosAbandonados(@Param("limite") Instant limite);
}
