package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.security.auth.dto.GoogleLoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.LoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.RegistroRequestDto;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import com.nahuel.ecommerce.dtos.UsuarioDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private TokenResponseDto tokenResponse() {
        UsuarioDto usuarioDto = new UsuarioDto();
        usuarioDto.setId(UUID.randomUUID());
        usuarioDto.setNombreUsuario("nahuel");
        usuarioDto.setEmail("nahuel@email.com");
        // campos opcionales/según tu DTO real
        usuarioDto.setRol(null);
        usuarioDto.setActivo(true);
        usuarioDto.setAccesoLocal(true);
        usuarioDto.setCreadoEn(java.time.Instant.now());

        return TokenResponseDto.builder()
                .token("jwt-token")
                .tipo("Bearer")
                .expiraEnSegundos(3600L)
                .usuario(usuarioDto)
                .build();
    }

    @Test
    @DisplayName("POST /auth/oauth - happy path")
    void loginOAuth_happyPath() {
        TokenResponseDto expected = tokenResponse();
        GoogleLoginRequestDto request = new GoogleLoginRequestDto("supabase-token");

        when(authService.loginOAuth("supabase-token")).thenReturn(expected);

        ResponseEntity<TokenResponseDto> response = authController.loginOAuth(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());

        verify(authService, times(1)).loginOAuth("supabase-token");
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/register - happy path")
    void registrar_happyPath() {
        TokenResponseDto expected = tokenResponse();
        RegistroRequestDto request = new RegistroRequestDto(
                "   USER@EMAIL.com  ",
                "Aa1aaaaa",
                "usuario1"
        );

        when(authService.registrar(request)).thenReturn(expected);

        ResponseEntity<TokenResponseDto> response = authController.registrar(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());

        verify(authService, times(1)).registrar(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/login - happy path")
    void login_happyPath() {
        TokenResponseDto expected = tokenResponse();
        LoginRequestDto request = new LoginRequestDto(
                "user@email.com",
                "Aa1aaaaa"
        );

        when(authService.loguear(request)).thenReturn(expected);

        ResponseEntity<TokenResponseDto> response = authController.login(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());

        verify(authService, times(1)).loguear(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/logout - happy path")
    void logout_happyPath() {
        doNothing().when(authService).logout();

        ResponseEntity<String> response = authController.logout();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Logout exitoso", response.getBody());

        verify(authService, times(1)).logout();
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/logout - propaga excepcion desde el service")
    void logout_propagatesException() {
        doThrow(new RuntimeException("boom")).when(authService).logout();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authController.logout());
        assertEquals("boom", ex.getMessage());

        verify(authService, times(1)).logout();
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/oauth - null request en controller llama igual al service (edge de controller unit)")
    void loginOAuth_nullRequest_callsServiceWithNull() {
        // Nota: en un entorno real, @Valid y el binding impedirían llegar acá,
        // pero en unit test puro del controller no ejecutamos el validador.
        TokenResponseDto expected = tokenResponse();
        when(authService.loginOAuth((String) isNull())).thenReturn(expected);

        ResponseEntity<TokenResponseDto> response = authController.loginOAuth((GoogleLoginRequestDto) null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());

        // Si tu NullPointerException aparece por request.tokenSupabase(),
        // este test fallará y te conviene ajustar: el controller debería evitar null o
        // hacer test usando MockMvc para validar.
        verifyNoMoreInteractions(authService);
    }
}
