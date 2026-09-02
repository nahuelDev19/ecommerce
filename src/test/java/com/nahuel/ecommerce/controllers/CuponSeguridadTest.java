package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.config.SecurityConfig;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.UserDetailsServiceImp;
import com.nahuel.ecommerce.services.CuponeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuponController.class)
@Import(SecurityConfig.class)
class CuponSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CuponeService cuponeService;

    @MockitoBean
    private UserDetailsServiceImp userDetailsServiceImp;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private final UUID idCupon = UUID.randomUUID();

    // =========================================================================
    // ACTOR 1: ANÓNIMO (No autenticado) -> Debe retornar 401 Unauthorized
    // =========================================================================

    @Test
    void usuarioAnonimo_alIntentarEliminarCupon_debeRetornarUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/cupones/{id}", idCupon))
                .andExpect(status().isUnauthorized());

        verify(cuponeService, never()).eliminarCuponPorId(any());
    }

    @Test
    void usuarioAnonimo_alIntentarCrearCupon_debeRetornarUnauthorized() throws Exception {
        String jsonValido = """
                {
                    "codigoCupon": "PROMO2026",
                    "descripcion": "Descuento de prueba",
                    "valorDescuento": 10,
                    "montoMinimoCarrito": 100,
                    "limiteUsoPorUsuario": 1
                }
                """;

        mockMvc.perform(post("/api/cupones")
                        .contentType(APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isUnauthorized());

        verify(cuponeService, never()).crearCupon(any());
    }

    // =========================================================================
    // ACTOR 2: ATACANTE / USUARIO SIN PERMISOS (ROLE_USER) -> Debe retornar 403 Forbidden
    // =========================================================================

    @Test
    @WithMockUser(username = "cliente", roles = {"USER"})
    void usuarioSinRolAdmin_alIntentarEliminarCupon_debeRetornarForbidden() throws Exception {
        mockMvc.perform(delete("/api/cupones/{id}", idCupon))
                .andExpect(status().isForbidden());

        verify(cuponeService, never()).eliminarCuponPorId(any());
    }

    @Test
    @WithMockUser(username = "cliente", roles = {"USER"})
    void usuarioSinRolAdmin_alIntentarCrearCupon_debeRetornarForbidden() throws Exception {
        String jsonValido = """
                {
                    "codigoCupon": "PROMO2026",
                    "descripcion": "Descuento de prueba",
                    "valorDescuento": 10,
                    "montoMinimoCarrito": 100,
                    "limiteUsoPorUsuario": 1
                }
                """;

        mockMvc.perform(post("/api/cupones")
                        .contentType(APPLICATION_JSON)
                        .content(jsonValido))
                .andExpect(status().isForbidden());

        verify(cuponeService, never()).crearCupon(any());
    }

    // =========================================================================
    // ACTOR 3: USUARIO AUTORIZADO (ROLE_ADMIN) -> Permite la operación
    // =========================================================================

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void usuarioAdmin_alEliminarCupon_debePermitirOperacion() throws Exception {
        when(cuponeService.eliminarCuponPorId(idCupon)).thenReturn(true);

        mockMvc.perform(delete("/api/cupones/{id}", idCupon))
                .andExpect(status().isNoContent());

        verify(cuponeService).eliminarCuponPorId(idCupon);
    }

    @Test
    @WithMockUser(username = "cliente", roles = {"USER"})
    void usuarioAutenticado_alConsultarCuponPorCodigo_debePermitirOperacion() throws Exception {
        String codigoCupon = "DESCUENTO10";

        when(cuponeService.buscarPorCodigo(codigoCupon)).thenReturn(null);

        mockMvc.perform(get("/api/cupones/cupones/{codigo}", codigoCupon))
                .andExpect(status().isOk());

        verify(cuponeService).buscarPorCodigo(codigoCupon);
    }
}