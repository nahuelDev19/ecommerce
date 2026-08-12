package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.AccesoDenegadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioAutenticadoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioAutenticadoService usuarioAutenticadoService;

    private Usuario usuario;

    private UUID usuarioId;

    @BeforeEach
    void setUp() {

        usuarioId = UUID.randomUUID();

        usuario = Usuario.builder()
                .id(usuarioId)
                .email("nahuel@gmail.com")
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();

        autenticarUsuario("nahuel@gmail.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================
    // ACTUAL()
    // =========================================================

    @Test
    void actual_deberiaDevolverUsuarioAutenticado() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioAutenticadoService.actual();

        assertNotNull(resultado);
        assertSame(usuario, resultado);

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void actual_deberiaBuscarUsuarioUsandoEmailDelAuthentication() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        usuarioAutenticadoService.actual();

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void actual_deberiaLanzarExcepcionSiUsuarioNoExiste() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.empty());

        RecursoNoEncontradoException exception = assertThrows(
                RecursoNoEncontradoException.class,
                () -> usuarioAutenticadoService.actual()
        );

        assertEquals(
                "El usuario no fue encontrado",
                exception.getMessage()
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    // =========================================================
    // VERIFICAR PROPIETARIO
    // =========================================================

    @Test
    void verificarPropietario_deberiaPermitirSiUsuarioEsPropietario() {

        usuario.setRol(Rol.PROPIETARIO);

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        assertDoesNotThrow(
                () -> usuarioAutenticadoService
                        .verificarPropietario(UUID.randomUUID())
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void verificarPropietario_deberiaPermitirSiUsuarioEsDuenioDelRecurso() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        assertDoesNotThrow(
                () -> usuarioAutenticadoService
                        .verificarPropietario(usuarioId)
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void verificarPropietario_deberiaDenegarSiUsuarioNoEsDuenio() {

        UUID otroUsuarioId = UUID.randomUUID();

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                AccesoDenegadoException.class,
                () -> usuarioAutenticadoService
                        .verificarPropietario(otroUsuarioId)
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void verificarPropietario_deberiaDenegarSiIdPropietarioEsNull() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                AccesoDenegadoException.class,
                () -> usuarioAutenticadoService
                        .verificarPropietario(null)
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    @Test
    void verificarPropietario_deberiaPropagarErrorSiUsuarioNoExiste() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> usuarioAutenticadoService
                        .verificarPropietario(usuarioId)
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void autenticarUsuario(String email) {

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}