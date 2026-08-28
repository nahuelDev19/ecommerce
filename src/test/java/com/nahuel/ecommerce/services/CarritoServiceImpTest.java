package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de servicio sobre CarritoServiceImp con caminos buenos y malos.
 *
 * El foco está en la autorización a nivel de negocio (IDOR / mass-assignment):
 * los tests que aquí se ven eran IMPOSIBLES de escribir antes del fix, porque
 * guardar()/listar() no recibían el usuario autenticado y confiaban en los
 * campos id/usuarioId que manda el cliente.
 */
@ExtendWith(MockitoExtension.class)
class CarritoServiceImpTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CarritoServiceImp carritoService;

    private UUID idUsuario;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        idUsuario = UUID.randomUUID();
        usuario = Usuario.builder()
                .id(idUsuario)
                .email("cliente@test.com")
                .build();
    }

    private Carrito carritoDe(UUID idCarrito, Usuario dueno) {
        Carrito carrito = new Carrito();
        carrito.setId(idCarrito);
        carrito.setUsuario(dueno);
        carrito.setEstadoCarrito(EstadoCarrito.ACTIVO);
        return carrito;
    }

    // =====================================================================
    // buscarPorId
    // =====================================================================

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Camino bueno: devuelve el carrito cuando pertenece al usuario")
        void buscarPorId_carritoPropio_devuelveCarrito() {
            UUID idCarrito = UUID.randomUUID();
            Carrito carrito = carritoDe(idCarrito, usuario);

            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.of(carrito));

            CarritoDto dto = carritoService.buscarPorId(idCarrito, idUsuario);

            assertNotNull(dto);
            assertEquals(idCarrito, dto.getId());
            assertEquals(idUsuario, dto.getUsuarioId());
        }

        @Test
        @DisplayName("Camino malo: carrito de OTRO usuario no se encuentra -> RecursoNoEncontrado")
        void buscarPorId_carritoDeOtroUsuario_lanzaRecursoNoEncontrado() {
            UUID idCarrito = UUID.randomUUID();
            UUID idOtroUsuario = UUID.randomUUID();

            // El repositorio solo encuentra carritos del dueño:
            // para un usuario distinto devuelve vacío y el servicio debe rechazar.
            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idOtroUsuario))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> carritoService.buscarPorId(idCarrito, idOtroUsuario));
        }
    }

    // =====================================================================
    // eliminar
    // =====================================================================

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        @DisplayName("Camino bueno: elimina el carrito propio")
        void eliminar_carritoPropio_elimina() {
            UUID idCarrito = UUID.randomUUID();
            Carrito carrito = carritoDe(idCarrito, usuario);

            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.of(carrito));

            carritoService.eliminar(idCarrito, idUsuario);

            verify(carritoRepository).delete(carrito);
        }

        @Test
        @DisplayName("Camino malo: carrito de otro usuario -> RecursoNoEncontrado y no se borra nada")
        void eliminar_carritoDeOtroUsuario_lanzaRecursoNoEncontrado() {
            UUID idCarrito = UUID.randomUUID();

            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> carritoService.eliminar(idCarrito, idUsuario));

            verify(carritoRepository, never()).delete(any());
        }
    }

    // =====================================================================
    // guardar (anti mass-assignment)
    // =====================================================================

    @Nested
    @DisplayName("guardar (anti mass-assignment)")
    class Guardar {

        @Test
        @DisplayName("Camino malo: el cliente manda id y usuarioId ajeno -> se ignoran; dueño = usuario autenticado")
        void guardar_dtoConIdYUsuarioAjeno_ignoraCamposDelCliente() {
            UUID idCarritoInventado = UUID.randomUUID();
            UUID idOtroUsuario = UUID.randomUUID();

            CarritoDto dto = new CarritoDto();
            dto.setId(idCarritoInventado);      // el cliente "elige" el id
            dto.setUsuarioId(idOtroUsuario);    // el cliente "elige" el dueño

            when(usuarioRepository.findById(idUsuario))
                    .thenReturn(Optional.of(usuario));
            when(carritoRepository.save(any(Carrito.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            carritoService.guardar(dto, idUsuario);

            ArgumentCaptor<Carrito> captor = ArgumentCaptor.forClass(Carrito.class);
            verify(carritoRepository).save(captor.capture());

            Carrito guardado = captor.getValue();
            assertNotEquals(idCarritoInventado, guardado.getId(),
                    "El id debe generarlo la BD, no tomarlo del cliente");
            assertSame(usuario, guardado.getUsuario(),
                    "El dueño debe ser el usuario autenticado, no el usuarioId del DTO");
        }

        @Test
        @DisplayName("Camino malo: el usuarioId del DTO nunca se consulta ni se usa")
        void guardar_noUsaElUsuarioDelDto() {
            UUID idOtroUsuario = UUID.randomUUID();

            CarritoDto dto = new CarritoDto();
            dto.setUsuarioId(idOtroUsuario);

            when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
            when(carritoRepository.save(any(Carrito.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            carritoService.guardar(dto, idUsuario);

            verify(usuarioRepository, never()).findById(idOtroUsuario);
            verify(usuarioRepository).findById(idUsuario);
        }

        @Test
        @DisplayName("Camino bueno: crea el carrito con estado ACTIVO por defecto")
        void guardar_carritoNuevoQuedaActivo() {
            CarritoDto dto = new CarritoDto();

            when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
            when(carritoRepository.save(any(Carrito.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CarritoDto resultado = carritoService.guardar(dto, idUsuario);

            assertEquals(EstadoCarrito.ACTIVO, resultado.getEstadoCarrito());
            assertEquals(idUsuario, resultado.getUsuarioId());
        }

        @Test
        @DisplayName("Camino malo: usuario autenticado inexistente -> RecursoNoEncontrado")
        void guardar_usuarioInexistente_lanzaRecursoNoEncontrado() {
            when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> carritoService.guardar(new CarritoDto(), idUsuario));

            verify(carritoRepository, never()).save(any());
        }
    }

    // =====================================================================
    // listar (scoping por usuario)
    // =====================================================================

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        @DisplayName("Camino malo: solo devuelve carritos del usuario autenticado (nunca findAll)")
        void listar_devuelveSoloCarritosDelUsuario() {
            Carrito propio = carritoDe(UUID.randomUUID(), usuario);

            when(carritoRepository.findByUsuarioId(idUsuario))
                    .thenReturn(List.of(propio));

            List<CarritoDto> resultado = carritoService.listar(idUsuario);

            assertEquals(1, resultado.size());
            assertEquals(idUsuario, resultado.get(0).getUsuarioId());
            verify(carritoRepository, never()).findAll();
        }

        @Test
        @DisplayName("Camino bueno: sin carritos devuelve lista vacía")
        void listar_sinCarritos_listaVacia() {
            when(carritoRepository.findByUsuarioId(idUsuario))
                    .thenReturn(List.of());

            List<CarritoDto> resultado = carritoService.listar(idUsuario);

            assertTrue(resultado.isEmpty());
        }
    }
}
