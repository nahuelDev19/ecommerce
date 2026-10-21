package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.AccesoDenegadoException;
import com.nahuel.ecommerce.exceptions.GlobalExceptionHandler;
import com.nahuel.ecommerce.services.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioIdorTest {

    @AfterEach
    void limpiarContextoDeSeguridad() {
        // El contexto pertenece al hilo: sin limpieza, otro test puede recibir
        // este usuario en lugar del usuario identificado por su propio JWT.
        SecurityContextHolder.clearContext();
    }

    private MockMvc mockMvc;
    private UsuarioService usuarioService;

    private UUID usuarioAutenticadoId;
    private UUID otroUsuarioId;

    @BeforeEach
    void setUp() {
        usuarioService = Mockito.mock(UsuarioService.class);
        UsuarioController controller = new UsuarioController(usuarioService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        usuarioAutenticadoId = UUID.randomUUID();
        otroUsuarioId = UUID.randomUUID();

        Usuario autenticado = Usuario.builder().id(usuarioAutenticadoId).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(autenticado, null, List.of())
        );
    }

    @Test
    void noDebePermitirConsultarUsuarioDeOtroUsuario() throws Exception {
        when(usuarioService.buscarPorId(otroUsuarioId, usuarioAutenticadoId))
                .thenThrow(new AccesoDenegadoException());

        mockMvc.perform(get("/api/usuarios/{id}", otroUsuarioId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void noDebePermitirEliminarUsuarioDeOtroUsuario() throws Exception {
        doThrow(new AccesoDenegadoException())
                .when(usuarioService).eliminar(otroUsuarioId, usuarioAutenticadoId);

        mockMvc.perform(delete("/api/usuarios/{id}", otroUsuarioId))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).eliminar(otroUsuarioId, otroUsuarioId);
    }
}
