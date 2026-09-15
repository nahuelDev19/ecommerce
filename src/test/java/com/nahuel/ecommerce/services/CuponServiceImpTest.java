package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.exceptions.CarritoNoModificableException;
import com.nahuel.ecommerce.exceptions.CuponExpiradoException;
import com.nahuel.ecommerce.exceptions.CuponInvalidoException;
import com.nahuel.ecommerce.exceptions.CuponNoVigenteException;
import com.nahuel.ecommerce.exceptions.CuponYaAplicadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.CuponesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de servicio sobre CuponServiceImp con caminos buenos y malos.
 *
 * quitarCupon(carritoId, idUsuario): antes del fix el método NO recibía el
 * usuario, así que no existía forma de rechazar un carrito ajeno. El camino
 * malo de abajo es el test que detecta ese IDOR.
 */
@ExtendWith(MockitoExtension.class)
class CuponServiceImpTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private CuponesRepository cuponesRepository;

    @InjectMocks
    private CuponServiceImp cuponService;

    private UUID idUsuario;
    private UUID idCarrito;
    private Carrito carrito;
    private Cupon cuponVigente;

    @BeforeEach
    void setUp() {
        idUsuario = UUID.randomUUID();
        idCarrito = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(idUsuario)
                .email("cliente@test.com")
                .build();

        carrito = new Carrito();
        carrito.setId(idCarrito);
        carrito.setUsuario(usuario);
        carrito.setEstadoCarrito(EstadoCarrito.ACTIVO);

        cuponVigente = new Cupon(
                "DESCUENTO10",
                "10% off",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                new BigDecimal("10"),
                TipoMoneda.ARG,
                new BigDecimal("1000.00"),
                1,
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
    }

    private Cupon cuponCon(EstadoCupon estado, Instant inicia, Instant termina) {
        return new Cupon(
                "DESCUENTO10",
                "desc",
                estado,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                new BigDecimal("10"),
                TipoMoneda.ARG,
                new BigDecimal("1000.00"),
                1,
                inicia,
                termina
        );
    }

    private ItemCarrito crearItem(BigDecimal precioUnitario, int cantidad) {
        Producto producto = Producto.builder()
                .id(UUID.randomUUID())
                .nombre("Producto Test")
                .precioBase(precioUnitario)
                .build();

        ItemCarrito item = new ItemCarrito();
        item.setId(UUID.randomUUID());
        item.setCarrito(carrito);
        item.setProducto(producto);
        item.setPrecioUnitario(precioUnitario);
        item.setCantidad(cantidad);
        return item;
    }

    // =====================================================================
    // aplicarCupon
    // =====================================================================

    @Nested
    @DisplayName("aplicarCupon")
    class AplicarCupon {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Aplica el cupón al carrito propio cuando se alcanza el monto mínimo")
            void aplicarCupon_carritoPropioCuponVigenteYMontoMinimoAlcanzado_aplicaCupon() {
                carrito.getItems().add(crearItem(new BigDecimal("1500.00"), 1));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));
                when(carritoRepository.save(any(Carrito.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                CarritoDto dto = cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario);

                assertNotNull(dto);
                assertEquals(idCarrito, dto.getId());
                verify(carritoRepository).save(carrito);
            }
        }

        @Nested
        @DisplayName("Camino malo")
        class CaminoMalo {

            @Test
            @DisplayName("Carrito de OTRO usuario -> RecursoNoEncontrado y no se consulta el cupón")
            void aplicarCupon_carritoDeOtroUsuario_lanzaRecursoNoEncontrado() {
                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.empty());

                assertThrows(RecursoNoEncontradoException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));

                verify(cuponesRepository, never()).findByCodigoCupon(any());
            }

            @Test
            @DisplayName("Cupón inactivo -> CuponInvalidoException")
            void aplicarCupon_cuponInactivo_lanzaCuponInvalido() {
                Cupon cuponInactivo = cuponCon(EstadoCupon.DESACTIVO,
                        Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponInactivo));

                assertThrows(CuponInvalidoException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            }

            @Test
            @DisplayName("Cupón expirado -> CuponExpiradoException")
            void aplicarCupon_cuponExpirado_lanzaCuponExpirado() {
                Cupon cuponExpirado = cuponCon(EstadoCupon.ACTIVO,
                        Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().minus(5, ChronoUnit.DAYS));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponExpirado));

                assertThrows(CuponExpiradoException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            }

            @Test
            @DisplayName("Cupón aún no vigente -> CuponNoVigenteException")
            void aplicarCupon_cuponNoVigente_lanzaCuponNoVigente() {
                Cupon cuponFuturo = cuponCon(EstadoCupon.ACTIVO,
                        Instant.now().plus(5, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponFuturo));

                assertThrows(CuponNoVigenteException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            }

            @Test
            @DisplayName("Carrito con cupón ya aplicado -> CuponYaAplicadoException")
            void aplicarCupon_carritoConCupon_lanzaCuponYaAplicado() {
                carrito.setCupon(cuponVigente);

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));

                assertThrows(CuponYaAplicadoException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            }

            @Test
            @DisplayName("Carrito abandonado -> CarritoNoModificableException")
            void aplicarCupon_carritoAbandonado_lanzaCarritoNoModificable() {
                carrito.setEstadoCarrito(EstadoCarrito.ABANDONADO);

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));

                assertThrows(CarritoNoModificableException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            }
            @Test
            @DisplayName("Carrito expirado -> CarritoNoModificableException")
            void aplicarCupon_carritoExpirado_lanzaCarritoNoModificable() {
                carrito.setEstadoCarrito(EstadoCarrito.EXPIRADO);

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));

                assertThrows(CarritoNoModificableException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            } @Test
            @DisplayName("Carrito ordenado -> CarritoNoModificableException")
            void aplicarCupon_carritoOrdenado_lanzaCarritoNoModificable() {
                carrito.setEstadoCarrito(EstadoCarrito.ORDENADO);

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));

                assertThrows(CarritoNoModificableException.class,
                        () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
            }

            @Test
            @DisplayName("Subtotal menor al monto mínimo exigido por el cupón -> rechaza la aplicación")
            void aplicarCupon_subtotalMenorAlMontoMinimo_lanzaReglaNegocioException() {
                carrito.getItems().add(crearItem(new BigDecimal("500.00"), 1));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));

                assertThatThrownBy(() -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario))
                        .as("No debe permitir aplicar el cupón si el subtotal del carrito no alcanza el monto mínimo exigido")
                        .isInstanceOf(ReglaNegocioException.class);

                verify(carritoRepository, never()).save(any(Carrito.class));
            }

            /*@Test
            @DisplayName("Bypass por reducción posterior: si el subtotal cae por debajo del mínimo, el descuento calculado debe ser cero")
            void calcularDescuento_subtotalCaePorDebajoDelMontoMinimo_descuentoEsCero() {
                carrito.setCupon(cuponVigente);
                carrito.getItems().add(crearItem(new BigDecimal("400.00"), 1));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(carritoRepository.save(any(Carrito.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                cuponService.quitarCupon(idCarrito, idUsuario);

                BigDecimal subtotalActual = new BigDecimal("400.00");
                BigDecimal descuentoCalculado = CuponServiceImp.calcularDescuento(carrito, subtotalActual);

                assertThat(descuentoCalculado)
                        .as("El descuento debe ser cero si el subtotal actual del carrito es menor al monto mínimo exigido por el cupón")
                        .isEqualByComparingTo(BigDecimal.ZERO);
            }
             */

            @Test
            @DisplayName("Bypass por reducción posterior: si el subtotal cae por debajo del mínimo, el descuento calculado debe ser cero")
            void calcularDescuento_subtotalCaePorDebajoDelMontoMinimo_descuentoEsCero() {
                // Given: Carrito que mantiene el cupón pero cuyo subtotal actual ($400.00) no alcanza el mínimo ($1000.00)
                carrito.setCupon(cuponVigente);
                BigDecimal subtotalReducido = new BigDecimal("400.00");

                // When: Se calcula el descuento sobre el subtotal reducido
                BigDecimal descuentoCalculado = CuponServiceImp.calcularDescuento(carrito, subtotalReducido);

                // Then: El descuento debe ser 0.00 independientemente de que el cupón siga asociado
                assertThat(descuentoCalculado)
                        .as("El descuento debe ser cero si el subtotal actual del carrito es menor al monto mínimo exigido por el cupón")
                        .isEqualByComparingTo(BigDecimal.ZERO);
            }

            @Test
            @DisplayName("Límite de uso por usuario alcanzado -> rechaza la aplicación del cupón")
            void aplicarCupon_limiteUsoPorUsuarioAlcanzado_lanzaReglaNegocioException() {
                carrito.getItems().add(crearItem(new BigDecimal("1500.00"), 1));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                        .thenReturn(Optional.of(cuponVigente));

                assertThatThrownBy(() -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario))
                        .as("No debe permitir aplicar un cupón si el usuario ya alcanzó el límite máximo de usos permitido")
                        .isInstanceOf(ReglaNegocioException.class);

                verify(carritoRepository, never()).save(any(Carrito.class));
            }

            @Test
            @DisplayName("Solicitudes concurrentes / apilamiento: rechaza la re-aplicación o sustitución de un cupón activo si la operación no es atómica")
            void aplicarCupon_apilamientoOReaplicacion_lanzaCuponYaAplicadoException() {
                carrito.setCupon(cuponVigente);
                carrito.getItems().add(crearItem(new BigDecimal("1500.00"), 1));

                when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("DESCUENTO20"))
                        .thenReturn(Optional.of(cuponCon(EstadoCupon.ACTIVO, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(1, ChronoUnit.DAYS))));

                assertThatThrownBy(() -> cuponService.aplicarCupon(idCarrito, "DESCUENTO20", idUsuario))
                        .as("No debe ser posible apilar o reemplazar un cupón en un carrito que ya posee uno aplicado")
                        .isInstanceOf(CuponYaAplicadoException.class);

                verify(carritoRepository, never()).save(any(Carrito.class));
            }
        }
    }

    // =====================================================================
    // quitarCupon
    // =====================================================================

    @Nested
    @DisplayName("quitarCupon (IDOR)")
    class QuitarCupon {

        @Test
        @DisplayName("Camino bueno: quita el cupón del carrito propio")
        void quitarCupon_carritoPropio_quitaCupon() {
            carrito.setCupon(cuponVigente);

            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.of(carrito));
            when(carritoRepository.save(any(Carrito.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CarritoDto dto = cuponService.quitarCupon(idCarrito, idUsuario);

            assertNotNull(dto);
            ArgumentCaptor<Carrito> captor = ArgumentCaptor.forClass(Carrito.class);
            verify(carritoRepository).save(captor.capture());
            assertNull(captor.getValue().getCupon());
        }

        @Test
        @DisplayName("Camino malo: quitar cupón de carrito de OTRO usuario -> RecursoNoEncontrado y no se modifica nada")
        void quitarCupon_carritoDeOtroUsuario_lanzaRecursoNoEncontrado() {
            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> cuponService.quitarCupon(idCarrito, idUsuario));

            verify(carritoRepository, never()).save(any());
        }
    }
}

/*
 =========================================================================================
 FILTRO OBLIGATORIO:
 1. "este test se pone rojo si alguien permite aplicar un cupón cuando el subtotal del carrito no alcanza el monto mínimo exigido"
 2. "este test se pone rojo si alguien otorga descuento en calcularDescuento cuando la modificación posterior del carrito deja el subtotal por debajo del monto mínimo"
 3. "este test se pone rojo si alguien permite aplicar un cupón habiendo superado el límite de uso por usuario definido en el cupón"
 4. "este test se pone rojo si alguien permite re-aplicar u otorgar múltiples cupones sobre el mismo carrito sin validar la presencia de un cupón previo"
 =========================================================================================
*/