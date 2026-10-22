package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Orden;
import com.nahuel.ecommerce.entitys.EstadoOrden;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface OrdenRepository extends JpaRepository<Orden, UUID> {

    Optional<Orden> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    List<Orden> findByUsuarioIdOrderByCreadoEnDesc(UUID usuarioId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orden o where o.id = :id")
    Optional<Orden> buscarPorIdParaActualizar(@Param("id") UUID id);

    @Modifying
    @Query("""
            update Orden o
            set o.estado = :cancelada, o.actualizadoEn = :ahora
            where o.estado = :pendiente
              and o.venceEn <= :ahora
            """)
    int cancelarOrdenesVencidas(
            @Param("pendiente") EstadoOrden pendiente,
            @Param("cancelada") EstadoOrden cancelada,
            @Param("ahora") Instant ahora
    );
}
