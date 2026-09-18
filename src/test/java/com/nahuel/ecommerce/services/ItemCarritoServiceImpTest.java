package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;
import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.mappers.CarritoMapper;
import com.nahuel.ecommerce.mappers.ItemCarritoMapper;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de servicio sobre ItemCarritoServiceImp con caminos buenos y malos.
 *
 * Los tres métodos ya reciben el id del usuario autenticado y resuelven el
 * carrito activo con findByUsuarioIdAndEstadoCarrito, así que el scoping por
 * usuario está cubierto; los caminos malos acá son los casos de recurso
 * inexistente, abuso de lógica (cantidades negativas) y cantidades inválidas.
 */
@ExtendWith(MockitoExtension.class)
class ItemCarritoServiceImpTest {

    @Mock
    private ItemCarritoRepository itemCarritoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CarritoService carritoService;

    @Spy
    private ItemCarritoMapper itemCarritoMapper = new ItemCarritoMapper();

    @Spy
    private CalculadoraCarrito calculadoraCarrito = new CalculadoraCarrito();

    @Spy
    private CarritoMapper carritoMapper = new CarritoMapper(itemCarritoMapper, calculadoraCarrito);

    @InjectMocks
    private ItemCarritoServiceImp itemCarritoService;

    private UUID idUsuario;
    private Usuario usuario;
    private UUID idProducto;
    private Producto producto;
    private Carrito carrito;

    @BeforeEach
    void setUp() {
        idUsuario = UUID.randomUUID();
        usuario = Usuario.builder()
                .id(idUsuario)
                .email("cliente@test.com")
                .build();

        idProducto = UUID.randomUUID();
        producto = Producto.builder()
                .id(idProducto)
                .nombre("Notebook")
                .precioBase(new BigDecimal("1500.00"))
                .build();

        carrito = new Carrito();
        carrito.setId(UUID.randomUUID());
        carrito.setUsuario(usuario);
        carrito.setEstadoCarrito(EstadoCarrito.ACTIVO);
    }

    private ItemRequestDto requestCantidad(int cantidad) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setProductoId(idProducto);
        dto.setCantidad(cantidad);
        return dto;
    }

    private ItemCarrito itemCon(int cantidad) {
        ItemCarrito item = new ItemCarrito();
        item.setId(UUID.randomUUID());
        item.setCarrito(carrito);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(producto.getPrecioBase());
        return item;
    }

    // =====================================================================
    // agregarItem
    // =====================================================================

    @Nested
    @DisplayName("agregarItem")
    class AgregarItem {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Sin carrito activo, crea el carrito y agrega el item")
            void agregarItem_sinCarritoActivo_creaCarritoYAgregaItem() {
                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
                when(productoRepository.findById(idProducto)).thenReturn(Optional.of(producto));
                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.empty());
                when(carritoRepository.save(any(Carrito.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                when(itemCarritoRepository.findByCarritoIdAndProductoId(any(), eq(idProducto)))
                        .thenReturn(Optional.empty());
                when(itemCarritoRepository.save(any(ItemCarrito.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                CarritoDto dto = itemCarritoService.agregarItem(requestCantidad(2), idUsuario);

                assertNotNull(dto);
                assertEquals(1, dto.getItems().size());
                assertEquals(idProducto, dto.getItems().get(0).getProductoId());
                assertEquals(2, dto.getItems().get(0).getCantidad());
                assertEquals(new BigDecimal("1500.00"), dto.getItems().get(0).getPrecioUnitario());
                assertEquals(new BigDecimal("3000.00"), dto.getItems().get(0).getSubtotal());
            }

            @Test
            @DisplayName("Si el item ya existe, incrementa la cantidad")
            void agregarItem_conItemExistente_incrementaCantidad() {
                ItemCarrito item = itemCon(3);

                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
                when(productoRepository.findById(idProducto)).thenReturn(Optional.of(producto));
                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));
                when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                        .thenReturn(Optional.of(item));

                itemCarritoService.agregarItem(requestCantidad(2), idUsuario);

                ArgumentCaptor<ItemCarrito> captor = ArgumentCaptor.forClass(ItemCarrito.class);
                verify(itemCarritoRepository).save(captor.capture());
                assertEquals(5, captor.getValue().getCantidad());
            }
        }

        @Nested
        @DisplayName("Camino malo")
        class CaminoMalo {

            @Test
            @DisplayName("Producto inexistente -> lanza excepción y no guarda nada")
            void agregarItem_productoInexistente_lanzaExcepcion() {
                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
                when(productoRepository.findById(idProducto)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class,
                        () -> itemCarritoService.agregarItem(requestCantidad(1), idUsuario));

                verify(itemCarritoRepository, never()).save(any());
            }

            @Test
            @DisplayName("Usuario inexistente -> lanza excepción")
            void agregarItem_usuarioInexistente_lanzaExcepcion() {
                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.empty());

                assertThrows(RuntimeException.class,
                        () -> itemCarritoService.agregarItem(requestCantidad(1), idUsuario));
            }

            @Test
            @DisplayName("Cantidad negativa en nuevo ítem -> rechaza y lanza ReglaNegocioException")
            void agregarItem_cantidadNegativaNuevoItem_lanzaReglaNegocioException() {
                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
                when(productoRepository.findById(idProducto)).thenReturn(Optional.of(producto));
                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));
                when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> itemCarritoService.agregarItem(requestCantidad(-5), idUsuario))
                        .as("Agregar un ítem con cantidad negativa debe ser rechazado por regla de negocio")
                        .isInstanceOf(ReglaNegocioException.class);

                verify(itemCarritoRepository, never()).save(any(ItemCarrito.class));
            }

            @Test
            @DisplayName("Cantidad cero en nuevo ítem -> rechaza y lanza ReglaNegocioException")
            void agregarItem_cantidadCero_lanzaReglaNegocioException() {
                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
                when(productoRepository.findById(idProducto)).thenReturn(Optional.of(producto));
                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));
                when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                        .thenReturn(Optional.empty());

                assertThatThrownBy(() -> itemCarritoService.agregarItem(requestCantidad(0), idUsuario))
                        .as("Agregar un ítem con cantidad cero debe ser rechazado antes de persistir")
                        .isInstanceOf(ReglaNegocioException.class);

                verify(itemCarritoRepository, never()).save(any(ItemCarrito.class));
            }

            @Test
            @DisplayName("Cantidad negativa en ítem existente -> rechaza inyección negativa")
            void agregarItem_cantidadNegativaEnItemExistente_lanzaReglaNegocioException() {
                ItemCarrito itemExistente = itemCon(10);

                when(usuarioRepository.findById(idUsuario)).thenReturn(Optional.of(usuario));
                when(productoRepository.findById(idProducto)).thenReturn(Optional.of(producto));
                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));
                when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                        .thenReturn(Optional.of(itemExistente));

                assertThatThrownBy(() -> itemCarritoService.agregarItem(requestCantidad(-3), idUsuario))
                        .as("No debe permitirse reducir stock o inyectar cantidades negativas vía agregarItem")
                        .isInstanceOf(ReglaNegocioException.class);

                verify(itemCarritoRepository, never()).save(any(ItemCarrito.class));
            }
        }
    }

    // =====================================================================
    // disminuirItem
    // =====================================================================

    @Nested
    @DisplayName("disminuirItem")
    class DisminuirItem {

        @Test
        @DisplayName("Camino bueno: reduce la cantidad sin llegar a cero")
        void disminuirItem_reduceCantidad() {
            ItemCarrito item = itemCon(5);

            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.of(carrito));
            when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                    .thenReturn(Optional.of(item));

            itemCarritoService.disminuirItem(requestCantidad(2), idUsuario);

            assertEquals(3, item.getCantidad());
            verify(itemCarritoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Camino malo: la cantidad llega a cero o menos -> elimina el item del carrito")
        void disminuirItem_cantidadAlcanzaCero_eliminaItem() {
            ItemCarrito item = itemCon(2);
            carrito.getItems().add(item);

            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.of(carrito));
            when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                    .thenReturn(Optional.of(item));

            itemCarritoService.disminuirItem(requestCantidad(2), idUsuario);

            verify(itemCarritoRepository).delete(item);
        }

        @Test
        @DisplayName("Camino malo: sin carrito activo -> RecursoNoEncontrado")
        void disminuirItem_sinCarritoActivo_lanzaRecursoNoEncontrado() {
            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> itemCarritoService.disminuirItem(requestCantidad(1), idUsuario));
        }

        @Test
        @DisplayName("Camino malo: item inexistente en el carrito -> RecursoNoEncontrado")
        void disminuirItem_itemInexistente_lanzaRecursoNoEncontrado() {
            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.of(carrito));
            when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> itemCarritoService.disminuirItem(requestCantidad(1), idUsuario));
        }

        @Test
        @DisplayName("Camino malo: cantidad menor o igual a cero -> rechazada")
        void disminuirItem_cantidadInvalida_lanzaExcepcion() {
            assertThrows(RuntimeException.class,
                    () -> itemCarritoService.disminuirItem(requestCantidad(0), idUsuario));

            verify(carritoRepository, never()).findByUsuarioIdAndEstadoCarrito(any(), any());
        }
    }

    // =====================================================================
    // eliminarItem
    // =====================================================================

    @Nested
    @DisplayName("eliminarItem")
    class EliminarItem {

        @Test
        @DisplayName("Camino bueno: elimina el item del carrito")
        void eliminarItem_eliminaItemDelCarrito() {
            ItemCarrito item = itemCon(3);
            carrito.getItems().add(item);

            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.of(carrito));
            when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                    .thenReturn(Optional.of(item));

            EliminarItemCarritoDto dto = new EliminarItemCarritoDto();
            dto.setProductoId(idProducto);

            itemCarritoService.eliminarItem(dto, idUsuario);

            verify(itemCarritoRepository).delete(item);
        }

        @Test
        @DisplayName("Camino malo: sin carrito activo -> RecursoNoEncontrado")
        void eliminarItem_sinCarritoActivo_lanzaRecursoNoEncontrado() {
            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.empty());

            EliminarItemCarritoDto dto = new EliminarItemCarritoDto();
            dto.setProductoId(idProducto);

            assertThrows(RecursoNoEncontradoException.class,
                    () -> itemCarritoService.eliminarItem(dto, idUsuario));
        }

        @Test
        @DisplayName("Camino malo: item inexistente en el carrito -> RecursoNoEncontrado")
        void eliminarItem_itemInexistente_lanzaRecursoNoEncontrado() {
            when(carritoRepository.findByUsuarioIdAndEstadoCarrito(idUsuario, EstadoCarrito.ACTIVO))
                    .thenReturn(Optional.of(carrito));
            when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), idProducto))
                    .thenReturn(Optional.empty());

            EliminarItemCarritoDto dto = new EliminarItemCarritoDto();
            dto.setProductoId(idProducto);

            assertThrows(RecursoNoEncontradoException.class,
                    () -> itemCarritoService.eliminarItem(dto, idUsuario));
        }
    }
}

/*
 =========================================================================================
 FILTRO OBLIGATORIO:
 1. "este test se pone rojo si alguien permite agregar un nuevo ítem al carrito con cantidad negativa"
 2. "este test se pone rojo si alguien permite agregar un ítem con cantidad igual a cero"
 3. "este test se pone rojo si alguien permite restar unidades o alterar ítems existentes enviando una cantidad negativa mediante agregarItem"
 =========================================================================================
*/