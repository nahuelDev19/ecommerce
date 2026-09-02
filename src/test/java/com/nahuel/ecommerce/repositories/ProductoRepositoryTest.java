package com.nahuel.ecommerce.repositories;

import com.nahuel.ecommerce.entitys.Producto;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Deshabilitado: @DataJpaTest requiere una base de datos embebida (ej. H2) no declarada en el pom.xml. " +
        "Condición de reactivación: agregar la dependencia h2 (scope test) en el pom.xml o configurar un perfil de BDD de pruebas.")
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void debeTratarPayloadDeInyeccionComoTexto() {
        Producto producto = new Producto(
                "Notebook Lenovo",
                "Producto de prueba",
                new BigDecimal("1000.00"),
                "ARS",
                true
        );

        productoRepository.saveAndFlush(producto);

        String payload = "' OR '1'='1";

        Page<Producto> resultado =
                productoRepository.buscarConFiltros(
                        payload,
                        null,
                        null,
                        true,
                        PageRequest.of(0, 20)
                );

        assertNotNull(resultado);

        // El payload no debe devolver el producto existente.
        // Debe ser tratado como texto literal.
        assertTrue(resultado.isEmpty());
    }

    @Test
    void noDebeDevolverTodosLosProductosConPayloadMalicioso() {
        Producto producto1 = new Producto(
                "Notebook",
                "Producto 1",
                new BigDecimal("1000.00"),
                "ARS",
                true
        );

        Producto producto2 = new Producto(
                "Celular",
                "Producto 2",
                new BigDecimal("500.00"),
                "ARS",
                true
        );

        productoRepository.save(producto1);
        productoRepository.save(producto2);
        productoRepository.flush();

        String payload = "' OR 1=1 --";

        Page<Producto> resultado =
                productoRepository.buscarConFiltros(
                        payload,
                        null,
                        null,
                        true,
                        PageRequest.of(0, 20)
                );

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        assertNotEquals(2, resultado.getTotalElements());
    }
}
