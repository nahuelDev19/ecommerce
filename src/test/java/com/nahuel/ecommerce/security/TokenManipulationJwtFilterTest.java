package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.support.JwtTestHelper;
import com.nahuel.ecommerce.support.JwtTestTokenValidator;
import com.nahuel.ecommerce.support.TestDataFactory;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas de Seguridad contra Token Manipulation (JWT)")
class TokenManipulationJwtFilterTest {

    private JwtTestHelper jwtTestHelper;
    private JwtTestTokenValidator jwtTestTokenValidator;
    private Usuario usuarioCliente;

    @BeforeEach
    void setUp() {
        jwtTestHelper = new JwtTestHelper();
        jwtTestTokenValidator = new JwtTestTokenValidator();
        usuarioCliente = TestDataFactory.usuarioCliente();
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
        @DisplayName("El rol ADMIN del token no reemplaza los permisos CLIENTE del usuario cargado")
        void tokenConRolAdmin_conservaPermisosDelUsuarioCargado() throws Exception {
            // Reemplaza las dos expectativas antiguas sobre el claim rol:
            // producción obtiene los permisos del usuario, no de ese claim.
            // Usamos servicio y filtro reales para comprobar esa regla.
            byte[] clave = "clave-ficticia-exclusiva-test-roles-0123456789abcdef"
                    .getBytes(StandardCharsets.UTF_8);
            JwtService servicioReal = new JwtService();
            ReflectionTestUtils.setField(servicioReal, "secretKey",
                    Base64.getEncoder().encodeToString(clave));

            // La firma es válida a propósito: aislamos el origen de los permisos.
            // No representa una alteración del token sin conocer la clave.
            String token = Jwts.builder()
                    .subject(usuarioCliente.getId().toString())
                    .claim("rol", "ADMIN")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 60000))
                    .signWith(Keys.hmacShaKeyFor(clave))
                    .compact();

            UsuarioRepository usuarios = mock(UsuarioRepository.class);
            UserDetailsService detalles = mock(UserDetailsService.class);
            when(usuarios.findById(usuarioCliente.getId())).thenReturn(Optional.of(usuarioCliente));
            when(detalles.loadUserByUsername(usuarioCliente.getEmail())).thenReturn(usuarioCliente);
            JwtFilter filtroReal = new JwtFilter(servicioReal, detalles, usuarios);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain cadena = mock(FilterChain.class);

            SecurityContextHolder.clearContext();
            try {
                filtroReal.doFilterInternal(request, response, cadena);

                var autenticacion = SecurityContextHolder.getContext().getAuthentication();
                assertNotNull(autenticacion);
                assertSame(usuarioCliente, autenticacion.getPrincipal());
                assertEquals(java.util.List.of("ROLE_CLIENTE"), autenticacion.getAuthorities()
                        .stream().map(a -> a.getAuthority()).toList());
                verify(usuarios).findById(usuarioCliente.getId());
                verify(detalles).loadUserByUsername(usuarioCliente.getEmail());
                verify(cadena).doFilter(request, response);
                assertEquals(200, response.getStatus());
            } finally {
                // Evita que la autenticación de este caso contamine otros tests.
                SecurityContextHolder.clearContext();
            }
        }
    }
}
