package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.exceptions.CarritoNoModificableException;
import com.nahuel.ecommerce.exceptions.CuponExpiradoException;
import com.nahuel.ecommerce.exceptions.CuponInvalidoException;
import com.nahuel.ecommerce.exceptions.CuponNoVigenteException;
import com.nahuel.ecommerce.exceptions.CuponYaAplicadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
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
                new BigDecimal("1000"),
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
                new BigDecimal("1000"),
                1,
                inicia,
                termina
        );
    }

    // =====================================================================
    // aplicarCupon
    // =====================================================================

    @Nested
    @DisplayName("aplicarCupon")
    class AplicarCupon {

        @Test
        @DisplayName("Camino bueno: aplica el cupón al carrito propio")
        void aplicarCupon_carritoPropioCuponVigente_aplicaCupon() {
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

        @Test
        @DisplayName("Camino malo: carrito de OTRO usuario -> RecursoNoEncontrado y no se consulta el cupón")
        void aplicarCupon_carritoDeOtroUsuario_lanzaRecursoNoEncontrado() {
            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));

            verify(cuponesRepository, never()).findByCodigoCupon(any());
        }

        @Test
        @DisplayName("Camino malo: cupón inactivo -> CuponInvalidoException")
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
        @DisplayName("Camino malo: cupón expirado -> CuponExpiradoException")
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
        @DisplayName("Camino malo: cupón aún no vigente -> CuponNoVigenteException")
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
        @DisplayName("Camino malo: carrito con cupón ya aplicado -> CuponYaAplicadoException")
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
        @DisplayName("Camino malo: carrito abandonado -> CarritoNoModificableException")
        void aplicarCupon_carritoAbandonado_lanzaCarritoNoModificable() {
            carrito.setEstadoCarrito(EstadoCarrito.ABANDONADO);

            when(carritoRepository.findByIdAndUsuarioId(idCarrito, idUsuario))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("DESCUENTO10"))
                    .thenReturn(Optional.of(cuponVigente));

            assertThrows(CarritoNoModificableException.class,
                    () -> cuponService.aplicarCupon(idCarrito, "DESCUENTO10", idUsuario));
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
