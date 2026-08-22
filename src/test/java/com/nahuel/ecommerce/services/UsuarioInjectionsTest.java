package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.AccesoDenegadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioInjectionsTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImp usuarioService;

    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioId = UUID.randomUUID();
    }

    @Test
    void debeTratarPayloadDeInyeccionComoTextoAlGuardar() {
        String payload = "' OR '1'='1";
        String payloadSql = "admin' OR 1=1 --";

        UsuarioDto dto = new UsuarioDto();
        dto.setEmail(payload);
        dto.setNombreUsuario(payloadSql);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setId(usuarioId);
                    return usuario;
                });

        UsuarioDto resultado = usuarioService.guardar(dto);

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(usuarioRepository).save(captor.capture());

        Usuario usuarioGuardado = captor.getValue();

        assertEquals(payload, usuarioGuardado.getEmail());
        assertEquals(payloadSql, usuarioGuardado.getNombreUsuario());

        assertEquals(payload, resultado.getEmail());
        assertEquals(payloadSql, resultado.getNombreUsuario());
    }

    @Test
    void debeBuscarPorIdSinConstruirConsultaConTextoDelUsuario() {
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("usuario@example.com")
                .nombreUsuario("usuario")
                .activo(true)
                .build();

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuario));

        UsuarioDto resultado =
                usuarioService.buscarPorId(usuarioId, usuarioId);

        verify(usuarioRepository).findById(usuarioId);
        verifyNoMoreInteractions(usuarioRepository);

        assertEquals("usuario@example.com", resultado.getEmail());
        assertEquals("usuario", resultado.getNombreUsuario());
    }

    @Test
    void debeRechazarAccesoAUnUsuarioDiferente() {
        UUID idSolicitado = UUID.randomUUID();
        UUID idAutenticado = UUID.randomUUID();

        assertThrows(
                AccesoDenegadoException.class,
                () -> usuarioService.buscarPorId(idSolicitado, idAutenticado)
        );

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void debeEliminarSoloAlUsuarioAutenticado() {
        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .email("usuario@example.com")
                .activo(true)
                .build();

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuario));

        usuarioService.eliminar(usuarioId, usuarioId);

        verify(usuarioRepository).findById(usuarioId);
        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void noDebeEliminarSiElUsuarioNoEstaAutenticado() {
        UUID idSolicitado = UUID.randomUUID();
        UUID idAutenticado = UUID.randomUUID();

        assertThrows(
                AccesoDenegadoException.class,
                () -> usuarioService.eliminar(idSolicitado, idAutenticado)
        );

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void debeInformarCuandoElUsuarioNoExiste() {
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> usuarioService.buscarPorId(usuarioId, usuarioId)
        );

        verify(usuarioRepository).findById(usuarioId);
    }
}
