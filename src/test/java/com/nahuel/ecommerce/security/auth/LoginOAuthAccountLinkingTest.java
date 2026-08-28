package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.UsuarioInactivoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.SupabaseTokenVerifier;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests de la vinculación OAuth (login con Google vía Supabase) con caminos
 * buenos y malos.
 *
 * Regla de seguridad que se protege aquí (C1):
 *  1. La identidad se resuelve por supabaseId, no por email.
 *  2. Una cuenta LOCAL (contraseña) NUNCA se enlaza automáticamente con OAuth:
 *     evita el pre-registro de un email ajeno para tomar el control de la cuenta.
 *  3. Un usuario desactivado no puede volver a entrar por OAuth.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Vinculación OAuth (C1): caminos buenos y malos")
class LoginOAuthAccountLinkingTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private SupabaseTokenVerifier supabaseTokenVerifier;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private AuthService authService;

    private static final String EMAIL = "victima@gmail.com";
    private static final String TOKEN = "supabase-token";
    private UUID supabaseId;

    @BeforeEach
    void setUp() {
        supabaseId = UUID.randomUUID();

        when(jwt.getClaimAsString("email")).thenReturn(EMAIL);
        when(jwt.getSubject()).thenReturn(supabaseId.toString());
        when(supabaseTokenVerifier.verificar(TOKEN)).thenReturn(jwt);
    }

    private Usuario usuarioGoogle(boolean activo) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .nombreUsuario("Victima")
                .activo(activo)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.GOOGLE)
                .supabaseId(supabaseId)
                .build();
    }

    private Usuario usuarioLocalSinSupabaseId() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .nombreUsuario("Atacante")
                .contrasena("password-del-atacante")
                .activo(true)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    private void stubGeneracionToken(Usuario usuario) {
        when(jwtService.generateToken(usuario)).thenReturn("jwt-token");
        when(jwtService.getExpirationInSeconds()).thenReturn(3600L);
    }

    // =====================================================================
    // CAMINOS BUENOS
    // =====================================================================

    @Nested
    @DisplayName("Caminos buenos")
    class CaminosBuenos {

        @Test
        @DisplayName("Cuenta GOOGLE existente identificada por supabaseId -> login OK")
        void oauth_cuentaGoogleExistentePorSupabaseId_loginOk() {
            Usuario usuarioGoogle = usuarioGoogle(true);
            when(usuarioRepository.findBySupabaseId(supabaseId))
                    .thenReturn(Optional.of(usuarioGoogle));
            stubGeneracionToken(usuarioGoogle);

            TokenResponseDto respuesta = authService.loginOAuth(TOKEN);

            assertNotNull(respuesta);
            assertEquals("jwt-token", respuesta.token());
            verify(usuarioRepository, never()).findByEmail(anyString());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cuenta GOOGLE legada sin supabaseId: se vincula por email y se completa el supabaseId")
        void oauth_cuentaGoogleSinSupabaseId_vinculaYLoginOk() {
            Usuario usuarioGoogleSinId = Usuario.builder()
                    .id(UUID.randomUUID())
                    .email(EMAIL)
                    .activo(true)
                    .rol(Rol.CLIENTE)
                    .origenRegistro(OrigenRegistro.GOOGLE)
                    .build();

            when(usuarioRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(usuarioGoogleSinId));
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            stubGeneracionToken(usuarioGoogleSinId);

            authService.loginOAuth(TOKEN);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertEquals(supabaseId, captor.getValue().getSupabaseId());
        }

        @Test
        @DisplayName("Sin cuenta existente: se crea cuenta GOOGLE con supabaseId -> login OK")
        void oauth_sinCuentaExistente_creaCuentaGoogle() {
            when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-token");
            when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

            TokenResponseDto respuesta = authService.loginOAuth(TOKEN);

            assertNotNull(respuesta);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());

            Usuario usuarioCreado = captor.getValue();
            assertEquals(EMAIL, usuarioCreado.getEmail());
            assertEquals(OrigenRegistro.GOOGLE, usuarioCreado.getOrigenRegistro());
            assertEquals(supabaseId, usuarioCreado.getSupabaseId());
            assertEquals(Rol.CLIENTE, usuarioCreado.getRol());
            assertTrue(usuarioCreado.getActivo());
        }
    }

    // =====================================================================
    // CAMINOS MALOS
    // =====================================================================

    @Nested
    @DisplayName("Caminos malos")
    class CaminosMalos {

        @Test
        @DisplayName("El email pertenece a una cuenta LOCAL: se rechaza y NO se enlaza (anti pre-registro)")
        void oauth_emailDeCuentaLocal_seRechazaSinEnlazar() {
            Usuario cuentaLocal = usuarioLocalSinSupabaseId();
            when(usuarioRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(cuentaLocal));

            BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                    () -> authService.loginOAuth(TOKEN));

            assertTrue(ex.getMessage().contains("creada con contraseña"));

            // La cuenta del atacante NO debe quedar vinculada ni recibir token.
            verify(usuarioRepository, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Usuario existente desactivado: se rechaza el login OAuth (UsuarioInactivoException)")
        void oauth_usuarioInactivo_seRechaza() {
            Usuario inactivo = usuarioGoogle(false);
            when(usuarioRepository.findBySupabaseId(supabaseId))
                    .thenReturn(Optional.of(inactivo));

            assertThrows(UsuarioInactivoException.class,
                    () -> authService.loginOAuth(TOKEN));

            verify(jwtService, never()).generateToken(any());
        }
    }
}
