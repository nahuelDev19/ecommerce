package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.services.CarritoService;
import com.nahuel.ecommerce.services.CuponeService;
import com.nahuel.ecommerce.services.ItemCarritoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del CarritoController centrados en que el usuario autenticado
 * se propague correctamente hacia los services (base para prevenir IDOR
 * a nivel controller).
 *
 * Notar que estos tests verifican que el CONTROLLER delega correctamente
 * el id del usuario autenticado a los services -- no verifican que el
 * SERVICE efectivamente rechace el acceso a un recurso ajeno. Esa
 * validación de negocio debe cubrirse con tests unitarios sobre la
 * implementación del service correspondiente (ver nota al final de la clase).
 */
@ExtendWith(MockitoExtension.class)
class CarritoIdorTest {

    @Mock
    private CarritoService carritoService;

    @Mock
    private ItemCarritoService itemCarritoService;

    @Mock
    private CuponeService cuponeService;

    @InjectMocks
    private CarritoController carritoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(carritoController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void controllerDebeEnviarElIdDelUsuarioAutenticadoAlBuscarCarrito() throws Exception {
        UUID idCarrito = UUID.randomUUID();
        UUID idUsuarioAutenticado = UUID.randomUUID();
        UUID idOtroUsuario = UUID.randomUUID();

        Usuario usuarioAutenticado = crearUsuario(idUsuarioAutenticado);
        SecurityContextHolder.getContext().setAuthentication(autenticar(usuarioAutenticado));

        CarritoDto carritoDeOtroUsuario = new CarritoDto();
        carritoDeOtroUsuario.setId(idCarrito);
        carritoDeOtroUsuario.setUsuarioId(idOtroUsuario);

        /*
         * Este mock simula al service devolviendo un carrito, sin importar
         * de quién sea -- acá NO se está probando la lógica de autorización
         * del service, solo que el controller reenvía el id correcto.
         */
        when(carritoService.buscarPorId(idCarrito, idUsuarioAutenticado))
                .thenReturn(carritoDeOtroUsuario);

        mockMvc.perform(get("/api/carritos/{id}", idCarrito))
                .andExpect(status().isOk());

        // El controller debe usar el id del usuario autenticado (no el de otro usuario)
        verify(carritoService).buscarPorId(idCarrito, idUsuarioAutenticado);
        verify(carritoService, never()).buscarPorId(idCarrito, idOtroUsuario);
    }

    @Test
    void debeEnviarElUsuarioAutenticadoAlEliminarCarrito() throws Exception {
        UUID id = UUID.randomUUID();
        UUID idUsuarioAutenticado = UUID.randomUUID();

        Usuario usuarioAutenticado = crearUsuario(idUsuarioAutenticado);
        SecurityContextHolder.getContext().setAuthentication(autenticar(usuarioAutenticado));

        mockMvc.perform(delete("/api/carritos/{id}", id))
                .andExpect(status().isOk());

        verify(carritoService).eliminar(id, idUsuarioAutenticado);
    }

    @Test
    void debeEnviarElUsuarioAutenticadoAlAplicarCupon() throws Exception {
        UUID idCarrito = UUID.randomUUID();
        UUID idUsuarioAutenticado = UUID.randomUUID();

        Usuario usuarioAutenticado = crearUsuario(idUsuarioAutenticado);
        SecurityContextHolder.getContext().setAuthentication(autenticar(usuarioAutenticado));

        CarritoDto respuesta = new CarritoDto();
        respuesta.setId(idCarrito);
        respuesta.setUsuarioId(idUsuarioAutenticado);

        when(cuponeService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuarioAutenticado))
                .thenReturn(respuesta);

        mockMvc.perform(put("/api/carritos/{idCarrito}/cupon/{codigo}", idCarrito, "DESCUENTO10"))
                .andExpect(status().isOk());

        verify(cuponeService).aplicarCupon(idCarrito, "DESCUENTO10", idUsuarioAutenticado);
    }

    @Test
    void debeObtenerElCarritoActivoDelUsuarioAutenticado() throws Exception {
        UUID idUsuarioAutenticado = UUID.randomUUID();

        Usuario usuarioAutenticado = crearUsuario(idUsuarioAutenticado);
        SecurityContextHolder.getContext().setAuthentication(autenticar(usuarioAutenticado));

        CarritoDto respuesta = new CarritoDto();
        respuesta.setUsuarioId(idUsuarioAutenticado);

        when(carritoService.buscarCarritoActivoDelUsuarioAutenticado(idUsuarioAutenticado))
                .thenReturn(respuesta);

        mockMvc.perform(get("/api/carritos/mi-carrito"))
                .andExpect(status().isOk());

        verify(carritoService).buscarCarritoActivoDelUsuarioAutenticado(idUsuarioAutenticado);
    }

    private Usuario crearUsuario(UUID idUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setEmail("usuario@test.com");
        usuario.setActivo(true);
        return usuario;
    }

    private Authentication autenticar(Usuario usuario) {
        return new UsernamePasswordAuthenticationToken(usuario, null, List.of());
    }

    /*
     * PENDIENTE: agregar un test sobre CarritoServiceImpl (mockeando
     * CarritoRepository) que verifique que buscarPorId/eliminar lanzan
     * una excepción (ej. ReglaNegocioException) cuando el carrito
     * encontrado no pertenece al usuario autenticado. Ese es el test
     * que realmente previene un IDOR a nivel de negocio.
     */
}