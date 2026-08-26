package com.nahuel.ecommerce.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.auth.dto.RegistroRequestDto;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Mitigación de Mass Assignment en el Registro de Usuarios")
class MassAssignmentRegisterTest {

    // Dependencias para probar la lógica real de AuthService
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // Mock independiente para aislar las pruebas HTTP de AuthController
    @Mock
    private AuthService authServiceMock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Inicializamos el controlador inyectándole el mock de AuthService
        AuthController authController = new AuthController(authServiceMock);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Nested
    @DisplayName("Ataques de Inyección de Campos Sensibles (Camino Malo / Ataque Bloqueado)")
    class PrivilegeEscalationAttacks {

        @Test
        @DisplayName("Ataque de Rol: Intento de inyectar 'rol: ADMIN' en el JSON asigna ROL_CLIENTE de todas formas")
        void registrar_IntentoInyeccionRolAdmin_DebeIgnorarPayloadYAsignarCliente() {
            // Arrange
            RegistroRequestDto request = new RegistroRequestDto();
            request.setEmail("atacante@test.com");
            request.setNombreUsuario("usuarioAtacante");
            request.setContrasena("Password123");

            when(usuarioRepository.findByEmail("atacante@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

            Usuario usuarioGuardado = Usuario.builder()
                    .id(UUID.randomUUID())
                    .email("atacante@test.com")
                    .nombreUsuario("usuarioAtacante")
                    .contrasena("encodedPassword")
                    .rol(Rol.CLIENTE)
                    .origenRegistro(OrigenRegistro.LOCAL)
                    .activo(true)
                    .build();

            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
            when(jwtService.generateToken(any())).thenReturn("mocked.jwt.token");

            // Act: Se prueba la lógica real de AuthService
            authService.registrar(request);

            // Assert: Capturamos la entidad enviada a la BD para auditar que el rol sea CLIENTE
            ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(usuarioCaptor.capture());

            Usuario usuarioPersistido = usuarioCaptor.getValue();
            assertEquals(Rol.CLIENTE, usuarioPersistido.getRol(), "El sistema NO debe permitir elevación de rol a ADMIN");
            assertNotEquals(Rol.ADMIN, usuarioPersistido.getRol());
        }

        @Test
        @DisplayName("Ataque de Deserialización HTTP: JSON malicioso con propiedades adicionales es ignorado por la API")
        void registrar_ConJsonPayloadConCamposInyectados_DebeAceptarPeticionEIgnorarCamposPrivilegiados() throws Exception {
            // Arrange: Enviamos un JSON con atributos inyectados (rol, activo, id)
            Map<String, Object> payloadMalicioso = new HashMap<>();
            payloadMalicioso.put("email", "atacante.http@test.com");
            payloadMalicioso.put("nombreUsuario", "atacanteHttp");
            payloadMalicioso.put("contrasena", "Password123");
            payloadMalicioso.put("rol", "ADMIN");
            payloadMalicioso.put("activo", false);
            payloadMalicioso.put("id", 9999L);

            TokenResponseDto responseDto = TokenResponseDto.builder()
                    .token("dummy.jwt.token")
                    .tipo("Bearer")
                    .expiraEnSegundos(3600L)
                    .build();

            when(authServiceMock.registrar(any(RegistroRequestDto.class))).thenReturn(responseDto);

            // Act & Assert: Verificamos que la petición responda 200 OK
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payloadMalicioso)))
                    .andExpect(status().isOk());

            // Auditamos que Spring MVC descarte las propiedades no mapeadas en RegistroRequestDto
            ArgumentCaptor<RegistroRequestDto> dtoCaptor = ArgumentCaptor.forClass(RegistroRequestDto.class);
            verify(authServiceMock).registrar(dtoCaptor.capture());

            RegistroRequestDto dtoMapeado = dtoCaptor.getValue();
            assertEquals("atacante.http@test.com", dtoMapeado.getEmail());
            assertEquals("atacanteHttp", dtoMapeado.getNombreUsuario());
        }
    }
}