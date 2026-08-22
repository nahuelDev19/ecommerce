package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.UsuarioInactivoException;
import com.nahuel.ecommerce.exceptions.UsuarioYaRegistradoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.SupabaseTokenVerifier;
import com.nahuel.ecommerce.security.auth.dto.LoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.RegistroRequestDto;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    private Usuario usuario;

    @BeforeEach
    void setUp() {

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("nahuel@gmail.com")
                .nombreUsuario("Nahuel")
                .contrasena("encoded-password")
                .activo(true)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();

        /*
        when(jwtService.generateToken(any(Usuario.class)))
                .thenReturn("jwt-token");

        when(jwtService.getExpirationInSeconds())
                .thenReturn(3600L);

         */
    }

    // =========================================================
    // REGISTRAR
    // =========================================================

    @Test
    void registrar_deberiaRegistrarUsuarioCorrectamente() {

        RegistroRequestDto request = new RegistroRequestDto();
        request.setEmail("  NAHUEL@GMAIL.COM ");
        request.setNombreUsuario("Nahuel");
        request.setContrasena("12345678");

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("12345678"))
                .thenReturn("password-encoded");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuario);

        when(jwtService.generateToken(usuario))          // <-- FALTABA ESTO
                .thenReturn("jwt-token");
        when(jwtService.getExpirationInSeconds())
                .thenReturn(3600L);
        TokenResponseDto resultado = authService.registrar(request);

        assertNotNull(resultado);
        assertEquals("jwt-token", resultado.token());
        assertEquals("Bearer", resultado.tipo());
        assertEquals(3600L, resultado.expiraEnSegundos());

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");

        verify(passwordEncoder)
                .encode("12345678");

        verify(usuarioRepository)
                .save(any(Usuario.class));

        verify(jwtService)
                .generateToken(usuario);
    }

    @Test
    void registrar_deberiaNormalizarEmailAntesDeGuardar() {

        RegistroRequestDto request = new RegistroRequestDto();
        request.setEmail("  NAHUEL@GMAIL.COM ");
        request.setNombreUsuario("Nahuel");
        request.setContrasena("12345678");

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuario);

        authService.registrar(request);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertEquals("nahuel@gmail.com", usuarioGuardado.getEmail());
        assertEquals("Nahuel", usuarioGuardado.getNombreUsuario());
        assertEquals(Rol.CLIENTE, usuarioGuardado.getRol());
        assertEquals(OrigenRegistro.LOCAL, usuarioGuardado.getOrigenRegistro());
        assertTrue(usuarioGuardado.getActivo());
        assertEquals("encoded", usuarioGuardado.getContrasena());
    }

    @Test
    void registrar_deberiaLanzarExcepcionSiEmailYaExiste() {

        RegistroRequestDto request = new RegistroRequestDto();
        request.setEmail("nahuel@gmail.com");
        request.setNombreUsuario("Nahuel");
        request.setContrasena("12345678");

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                UsuarioYaRegistradoException.class,
                () -> authService.registrar(request)
        );

        verify(usuarioRepository, never())
                .save(any());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(jwtService, never())
                .generateToken(any());
    }

    // =========================================================
    // LOGUEAR
    // =========================================================

    @Test
    void loguear_deberiaAutenticarUsuarioCorrectamente() {

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("NAHUEL@GMAIL.COM");
        request.setContrasena("12345678");

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario))          // <-- FALTABA ESTO
                .thenReturn("jwt-token");
        TokenResponseDto resultado = authService.loguear(request);

        assertNotNull(resultado);
        assertEquals("jwt-token", resultado.token());
        assertEquals("Bearer", resultado.tipo());

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");

        verify(jwtService)
                .generateToken(usuario);
    }

    @Test
    void loguear_deberiaNormalizarEmailAntesDeBuscarUsuario() {

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("  NAHUEL@GMAIL.COM ");
        request.setContrasena("12345678");

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        authService.loguear(request);

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void loguear_deberiaLanzarBadCredentialsSiCredencialesSonIncorrectas() {

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("nahuel@gmail.com");
        request.setContrasena("incorrecta");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("error"));


        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.loguear(request)
        );

        assertEquals(
                "username o password incorrectos",
                exception.getMessage()
        );

        verify(usuarioRepository, never())
                .findByEmail(anyString());

        verify(jwtService, never())
                .generateToken(any());
    }

    @Test
    void loguear_deberiaLanzarExcepcionSiUsuarioEstaInactivo() {

        Usuario usuarioInactivo = Usuario.builder()
                .id(UUID.randomUUID())
                .email("nahuel@gmail.com")
                .nombreUsuario("Nahuel")
                .activo(false)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("nahuel@gmail.com");
        request.setContrasena("12345678");

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuarioInactivo));

        assertThrows(
                UsuarioInactivoException.class,
                () -> authService.loguear(request)
        );

        verify(jwtService, never())
                .generateToken(any());
    }

    // =========================================================
    // LOGIN OAUTH
    // =========================================================

    @Test
    void loginOAuth_deberiaLoguearUsuarioExistente() {

        UUID supabaseId = UUID.randomUUID();

        when(jwt.getClaimAsString("email"))
                .thenReturn("  NAHUEL@GMAIL.COM ");

        when(jwt.getSubject())
                .thenReturn(supabaseId.toString());

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);


        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario))          // <-- FALTABA ESTO
                .thenReturn("jwt-token");
        TokenResponseDto resultado =
                authService.loginOAuth("supabase-token");

        assertNotNull(resultado);
        assertEquals("jwt-token", resultado.token());
        assertEquals("Bearer", resultado.tipo());

        verify(supabaseTokenVerifier)
                .verificar("supabase-token");

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");

        verify(usuarioRepository, never())
                .save(any());

        verify(jwtService)
                .generateToken(usuario);
    }

    @Test
    void loginOAuth_deberiaCrearUsuarioSiNoExiste() {

        UUID supabaseId = UUID.randomUUID();

        when(jwt.getClaimAsString("email"))
                .thenReturn("NAHUEL@GMAIL.COM");

        when(jwt.getSubject())
                .thenReturn(supabaseId.toString());

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.empty());

        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(usuario);

        TokenResponseDto resultado =
                authService.loginOAuth("supabase-token");

        assertNotNull(resultado);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioCreado = captor.getValue();

        assertEquals("nahuel@gmail.com", usuarioCreado.getEmail());
        assertEquals(OrigenRegistro.GOOGLE, usuarioCreado.getOrigenRegistro());
        assertEquals(supabaseId, usuarioCreado.getSupabaseId());
        assertEquals(Rol.CLIENTE, usuarioCreado.getRol());
        assertTrue(usuarioCreado.getActivo());
    }

    @Test
    void loginOAuth_deberiaLanzarExcepcionSiEmailNoExisteEnToken() {

        when(jwt.getClaimAsString("email"))
                .thenReturn(null);

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.loginOAuth("supabase-token")
        );

        verify(usuarioRepository, never())
                .findByEmail(anyString());

        verify(usuarioRepository, never())
                .save(any());
    }

    @Test
    void loginOAuth_deberiaLanzarExcepcionSiEmailEstaVacio() {

        when(jwt.getClaimAsString("email"))
                .thenReturn("   ");

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.loginOAuth("supabase-token")
        );

        verify(usuarioRepository, never())
                .findByEmail(anyString());
    }

    @Test
    void loginOAuth_deberiaLanzarExcepcionSiSubjectEsNull() {

        when(jwt.getClaimAsString("email"))
                .thenReturn("nahuel@gmail.com");

        when(jwt.getSubject())
                .thenReturn(null);

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.loginOAuth("supabase-token")
        );

        verify(usuarioRepository, never())
                .findByEmail(anyString());
    }

    @Test
    void loginOAuth_deberiaLanzarExcepcionSiSubjectEstaVacio() {

        when(jwt.getClaimAsString("email"))
                .thenReturn("nahuel@gmail.com");

        when(jwt.getSubject())
                .thenReturn("   ");

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.loginOAuth("supabase-token")
        );

        verify(usuarioRepository, never())
                .findByEmail(anyString());
    }

    @Test
    void loginOAuth_deberiaLanzarExcepcionSiSubjectNoEsUUID() {

        when(jwt.getClaimAsString("email"))
                .thenReturn("nahuel@gmail.com");

        when(jwt.getSubject())
                .thenReturn("uuid-invalido");

        when(supabaseTokenVerifier.verificar("supabase-token"))
                .thenReturn(jwt);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.loginOAuth("supabase-token")
        );

        assertEquals(
                "El subject de Supabase no es un UUID válido",
                exception.getMessage()
        );

        verify(usuarioRepository, never())
                .findByEmail(anyString());
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @Test
    void logout_deberiaLanzarExcepcionSiAuthenticationEsNull() {

        SecurityContextHolder.clearContext();

        assertThrows(
                BadCredentialsException.class,
                () -> authService.logout()
        );
    }

    @Test
    void logout_deberiaLanzarExcepcionSiAuthenticationNoEstaAutenticado() {

        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated())
                .thenReturn(false);

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.logout()
        );
    }

    @Test
    void logout_deberiaLanzarExcepcionSiAuthenticationEsAnonimo() {

        Authentication authentication =
                mock(AnonymousAuthenticationToken.class);

        when(authentication.isAuthenticated())
                .thenReturn(true);

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.logout()
        );
    }

    @Test
    void logout_deberiaLanzarExcepcionSiUsuarioEsAnonymousUser() {

        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated())
                .thenReturn(true);

        when(authentication.getName())
                .thenReturn("anonymousUser");

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.logout()
        );
    }

    @Test
    void logout_deberiaPermitirLogoutConUsuarioAutenticado() {

        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated())
                .thenReturn(true);

        when(authentication.getName())
                .thenReturn("nahuel@gmail.com");

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        assertDoesNotThrow(
                () -> authService.logout()
        );
    }
}