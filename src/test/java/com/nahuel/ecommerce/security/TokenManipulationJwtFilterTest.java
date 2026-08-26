package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.support.JwtTestHelper;
import com.nahuel.ecommerce.support.JwtTestTokenValidator;
import com.nahuel.ecommerce.support.TestDataFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Seguridad contra Token Manipulation (JWT)")
class TokenManipulationJwtFilterTest {

    private JwtTestHelper jwtTestHelper;
    private JwtTestTokenValidator jwtTestTokenValidator;
    private Usuario usuarioCliente;
    private Usuario usuarioAdmin;

    @BeforeEach
    void setUp() {
        jwtTestHelper = new JwtTestHelper();
        jwtTestTokenValidator = new JwtTestTokenValidator();
        usuarioCliente = TestDataFactory.usuarioCliente();
        usuarioAdmin = TestDataFactory.usuarioAdmin();
    }

    @Nested
    @DisplayName("Validaciones de Integridad y Firma de Token")
    class IntegrityAndSignatureTests {

        @Test
        @DisplayName("Firma Inválida: Token firmado con clave secreta no autorizada debe fallar la validación")
        void parsearClaims_ConFirmaInvalida_DebeLanzarJwtException() {
            // Arrange: Generamos un token firmado con una clave distinta
            String tokenManipulado = jwtTestHelper.tokenConFirmaInvalida(usuarioCliente);

            // Act & Assert: El validador debe rechazar la firma manipulada
            assertThrows(
                    JwtException.class,
                    () -> jwtTestTokenValidator.parsearClaims(tokenManipulado)
            );
            assertFalse(jwtTestTokenValidator.esValido(tokenManipulado));
        }

        @Test
        @DisplayName("Expiración Manipulada: Token vencido debe rechazarse")
        void parsearClaims_ConTokenExpirado_DebeLanzarJwtException() {
            // Arrange
            String tokenExpirado = jwtTestHelper.tokenExpirado(usuarioCliente);

            // Act & Assert
            assertThrows(
                    JwtException.class,
                    () -> jwtTestTokenValidator.parsearClaims(tokenExpirado)
            );
            assertFalse(jwtTestTokenValidator.esValido(tokenExpirado));
        }
    }

    @Nested
    @DisplayName("Validaciones de Privilegios y Claims (Escalación de Roles)")
    class RoleAndPrivilegeManipulationTests {

        @Test
        @DisplayName("Verificación de Claims: Token legítimo de cliente debe conservar su rol de CLIENTE")
        void parsearClaims_ConUsuarioClienteLegitimo_DebeContenerRolCliente() {
            // Arrange
            String tokenCliente = jwtTestHelper.tokenParaUsuario(usuarioCliente);

            // Act
            Claims claims = jwtTestTokenValidator.parsearClaims(tokenCliente);
            String rolExtraido = jwtTestTokenValidator.extraerRol(tokenCliente);

            // Assert
            assertTrue(jwtTestTokenValidator.esValido(tokenCliente));
            assertEquals("CLIENTE", rolExtraido);
            assertEquals("CLIENTE", claims.get("rol", String.class));
            assertNotEquals("ADMIN", rolExtraido);
        }

        @Test
        @DisplayName("Aislamiento de Roles: Token legítimo de admin debe certificar el rol ADMIN")
        void parsearClaims_ConUsuarioAdminLegitimo_DebeContenerRolAdmin() {
            // Arrange
            String tokenAdmin = jwtTestHelper.tokenParaUsuario(usuarioAdmin);

            // Act
            String rolExtraido = jwtTestTokenValidator.extraerRol(tokenAdmin);

            // Assert
            assertTrue(jwtTestTokenValidator.esValido(tokenAdmin));
            assertEquals("ADMIN", rolExtraido);
        }
    }
}