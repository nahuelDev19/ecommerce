package com.nahuel.ecommerce.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.exceptions.GlobalExceptionHandler;
import com.nahuel.ecommerce.security.auth.dto.LoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Contrato HTTP de User Enumeration en AuthController")
class LoginUserEnumerationAuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Registramos tu GlobalExceptionHandler para que MockMvc sepa cómo interceptar la excepción
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /auth/login - Credenciales válidas devuelven HTTP 200 OK")
    void login_ConCredencialesValidas_DebeRetornarHttp200Ok() throws Exception {
        LoginRequestDto request = new LoginRequestDto("user@test.com", "Password123");
        TokenResponseDto tokenResponse = TokenResponseDto.builder()
                .token("jwt.dummy.token")
                .tipo("Bearer")
                .expiraEnSegundos(3600L)
                .usuario(new UsuarioDto())
                .build();

        when(authService.loguear(any(LoginRequestDto.class))).thenReturn(tokenResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.dummy.token"));
    }

    @Test
    @DisplayName("POST /auth/login - Credenciales erróneas devuelven HTTP 401 Unauthorized y mensaje unificado")
    void login_ConCredencialesErroneas_DebeRetornarHttp401Unauthorized() throws Exception {
        LoginRequestDto request = new LoginRequestDto("user@test.com", "Password123");

        when(authService.loguear(any(LoginRequestDto.class)))
                .thenThrow(new BadCredentialsException("username o password incorrectos"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("username o password incorrectos"));
    }
}