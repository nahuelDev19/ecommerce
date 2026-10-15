package com.nahuel.ecommerce.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer; // Testcontainers 2.x (no org.testcontainers.containers)

@Testcontainers // maneja el ciclo de vida de los contenedores
@SpringBootTest // levanta el contexto real de Spring
@ActiveProfiles("test") // jwt.secret y supabase.url salen de application-test.yml
public abstract class TestDeIntegracion {

    // @Container es obligatorio: sin esta anotacion @Testcontainers ignora el campo,
    // el contenedor nunca arranca y @ServiceConnection falla al leer el puerto mapeado.
    // Este contenedor lo van a usar todos los tests que hereden
    @Container
    @ServiceConnection // Spring Boot toma url, user, pass de acá y configura el DataSource
    // En Testcontainers 2.x PostgreSQLContainer ya NO es generico: sin <?> ni <>.
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    // Por qué static? ->
    /*
     * El campo tiene que ser static porque:
     * 1. Se comparte entre todos los tests que heredan de esta clase.
     *    Así el contenedor se inicia UNA sola vez y no una vez por test.
     * 2. @ServiceConnection + @Testcontainers necesita que el contenedor esté
     *    disponible ANTES de que Spring cree el ApplicationContext.
     *    Si no es static, JUnit lo crea después y Spring no puede conectarse.
     */
}
