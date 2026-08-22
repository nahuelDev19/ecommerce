package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.config.SecurityConfig;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.pipeline.ProcesarArchivosService;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.UserDetailsServiceImp;
import com.nahuel.ecommerce.services.ProductoServiceImp;
import com.nahuel.ecommerce.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
class ProductoSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoServiceImp productoService;
    @MockitoBean
    private UserDetailsServiceImp userDetailsService; // en vez de UserDetailsService

    @MockitoBean
    private ProcesarArchivosService procesarArchivosService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;


    private Usuario cliente;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        cliente = TestDataFactory.usuarioCliente();
        admin = TestDataFactory.usuarioAdmin();

        // el string del token no importa: JwtService está mockeado,
        // así que "controlamos" lo que devuelve sin depender de firma/parseo real
        when(jwtService.extractSubject("token-cliente")).thenReturn(cliente.getId().toString());
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(userDetailsService.loadUserByUsername(cliente.getEmail())).thenReturn(cliente);
        when(jwtService.isTokenValid("token-cliente", cliente)).thenReturn(true);

        when(jwtService.extractSubject("token-admin")).thenReturn(admin.getId().toString());
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userDetailsService.loadUserByUsername(admin.getEmail())).thenReturn(admin);
        when(jwtService.isTokenValid("token-admin", admin)).thenReturn(true);
    }

    @Test
    void clienteNoPuedeCrearProducto() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer token-cliente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(productoService, never()).guardarProducto(any());
    }

    @Test
    void clienteNoPuedeEliminarProducto() throws Exception {
        mockMvc.perform(delete("/api/productos/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer token-cliente"))
                .andExpect(status().isForbidden());

        verify(productoService, never()).descontinuarProducto(any());
    }

    @Test
    void sinTokenRecibeNoAutorizado() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRecibeNoAutorizado() throws Exception {
        when(jwtService.extractSubject("token-basura")).thenReturn(null);

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer token-basura")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

}