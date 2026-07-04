package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {


    @Query("select p from Producto p where p.activo=false")
    List<Producto> findAllDesactivos();

    @Query("""
    SELECT p
    FROM Producto p
    WHERE(:soloActivos= false OR p.activo=true)
    AND (CAST(:nombre AS string) IS NULL OR LOWER(p.nombre) LIKE CONCAT('%', LOWER(CAST(:nombre AS string)), '%'))
    AND (:precioMin IS NULL OR p.precioBase >= :precioMin)
    AND (:precioMax IS NULL OR p.precioBase <= :precioMax)
""")
    Page<Producto> buscarConFiltros(
            @Param("nombre") String nombre,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax,
            @Param("soloActivo") boolean soloActivo,
            Pageable pageable
    );
}
