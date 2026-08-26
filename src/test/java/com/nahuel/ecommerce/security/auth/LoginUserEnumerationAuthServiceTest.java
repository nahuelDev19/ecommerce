package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.auth.dto.LoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Mitigación de User Enumeration en AuthService")
class LoginUserEnumerationAuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private LoginRequestDto loginRequestDto;
    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        loginRequestDto = new LoginRequestDto("user@test.com", "Password123");

        usuarioMock = Usuario.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .nombreUsuario("userTest")
                .contrasena("encodedPassword")
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("Camino Feliz: Credenciales válidas retornan token exitosamente")
    void loguear_ConCredencialesValidas_DebeRetornarTokenResponseDto() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(usuarioRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(usuarioMock));
        when(jwtService.generateToken(usuarioMock))
                .thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationInSeconds())
                .thenReturn(3600L);

        // Act
        TokenResponseDto response = authService.loguear(loginRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.token());
        assertEquals("user@test.com", response.usuario().getEmail());
    }

    @Nested
    @DisplayName("Validación Anti-User-Enumeration (Caminos Malos Unificados)")
    class AntiUserEnumerationProtectionTests {

        @Test
        @DisplayName("Usuario Inexistente: Lanza BadCredentialsException con mensaje genérico")
        void loguear_ConUsuarioInexistente_DebeLanzarMismaExcepcionYMensajeGenerico() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            BadCredentialsException exception = assertThrows(
                    BadCredentialsException.class,
                    () -> authService.loguear(loginRequestDto)
            );

            // Verificamos que no revele que el usuario no existe
            assertEquals("username o password incorrectos", exception.getMessage());
            verify(usuarioRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Contraseña Incorrecta: Lanza BadCredentialsException con mensaje genérico idéntico")
        void loguear_ConContrasenaIncorrecta_DebeLanzarMismaExcepcionYMensajeGenerico() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            BadCredentialsException exception = assertThrows(
                    BadCredentialsException.class,
                    () -> authService.loguear(loginRequestDto)
            );

            // El mensaje devuelto debe ser exacto al caso de usuario inexistente
            assertEquals("username o password incorrectos", exception.getMessage());
            verify(usuarioRepository, never()).findByEmail(anyString());
        }
    }
}