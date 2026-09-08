package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.CarritoNoModificableException;
import com.nahuel.ecommerce.exceptions.CuponExpiradoException;
import com.nahuel.ecommerce.exceptions.CuponInvalidoException;
import com.nahuel.ecommerce.exceptions.CuponNoVigenteException;
import com.nahuel.ecommerce.exceptions.CuponYaAplicadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.CuponesRepository;
import com.nahuel.ecommerce.support.CarritoTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de validación de reglas de negocio para aplicación de cupones")
class CuponAplicacionReglasTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private CuponesRepository cuponesRepository;

    @InjectMocks
    private CuponServiceImp cuponServiceImp;

    private Usuario ana;
    private Producto productoMil;

    @BeforeEach
    void setUp() {
        ana = new Usuario();
        ana.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        ana.setNombreUsuario("Ana");

        productoMil = new Producto();
        productoMil.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        productoMil.setNombre("Producto $1000");
        productoMil.setPrecioBase(new BigDecimal("1000.00"));
    }

    @Nested
    @DisplayName("Camino bueno")
    class CaminoBueno {

        @Test
        @DisplayName("Aplicar cupón vigente a carrito activo sin cupón asocia el cupón y guarda los cambios")
        void aplicarCupon_cuponVigenteYCarritoActivo_aplicaExitosamente() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
            Cupon cuponVagente = CarritoTestFactory.cuponPorcentaje(new BigDecimal("10.00"));

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("PORCENTAJE10"))
                    .thenReturn(Optional.of(cuponVagente));
            when(carritoRepository.save(any(Carrito.class)))
                    .thenReturn(carrito);

            // When
            CarritoDto resultado = cuponServiceImp.aplicarCupon(carrito.getId(), "PORCENTAJE10", ana.getId());

            // Then
            assertThat(resultado.getDescuentoTotal())
                    .as("El descuento aplicado debe ser del 10% ($100.00)")
                    .isEqualByComparingTo(new BigDecimal("100.00"));

            assertThat(carrito.getCupon())
                    .as("El cupón debe quedar asociado a la entidad carrito")
                    .isEqualTo(cuponVagente);

            verify(carritoRepository).save(carrito);
        }
    }

    @Nested
    @DisplayName("Camino malo (Validaciones y Reglas de Negocio)")
    class CaminoMalo {

        @Test
        @DisplayName("Aplicar cupón inactivo lanza CuponInvalidoException y no guarda el carrito")
        void aplicarCupon_cuponInactivo_lanzaCuponInvalidoException() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
            Cupon cuponInactivo = CarritoTestFactory.cuponInactivo();

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("INACTIVO"))
                    .thenReturn(Optional.of(cuponInactivo));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.aplicarCupon(carrito.getId(), "INACTIVO", ana.getId()))
                    .as("No se permite aplicar cupones con estado DESACTIVO")
                    .isInstanceOf(CuponInvalidoException.class);

            verify(carritoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Aplicar cupón que aún no inicia su vigencia lanza CuponNoVigenteException")
        void aplicarCupon_cuponFuturo_lanzaCuponNoVigenteException() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
            Cupon cuponFuturo = CarritoTestFactory.cuponNoVigente();

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("FUTURO"))
                    .thenReturn(Optional.of(cuponFuturo));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.aplicarCupon(carrito.getId(), "FUTURO", ana.getId()))
                    .as("No se permite aplicar cupones cuya fecha de inicio es posterior a la actual")
                    .isInstanceOf(CuponNoVigenteException.class);

            verify(carritoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Aplicar cupón vencido lanza CuponExpiradoException")
        void aplicarCupon_cuponExpirado_lanzaCuponExpiradoException() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
            Cupon cuponExpirado = CarritoTestFactory.cuponExpirado();

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("EXPIRADO"))
                    .thenReturn(Optional.of(cuponExpirado));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.aplicarCupon(carrito.getId(), "EXPIRADO", ana.getId()))
                    .as("No se permite aplicar cupones cuya fecha de término ya pasó")
                    .isInstanceOf(CuponExpiradoException.class);

            verify(carritoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Aplicar cupón a un carrito abandonado lanza CarritoNoModificableException")
        void aplicarCupon_carritoAbandonado_lanzaCarritoNoModificableException() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
            carrito.setEstadoCarrito(EstadoCarrito.ABANDONADO);
            Cupon cuponVagente = CarritoTestFactory.cuponPorcentaje(new BigDecimal("10.00"));

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("PORCENTAJE10"))
                    .thenReturn(Optional.of(cuponVagente));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.aplicarCupon(carrito.getId(), "PORCENTAJE10", ana.getId()))
                    .as("No se pueden modificar carritos en estado ABANDONADO")
                    .isInstanceOf(CarritoNoModificableException.class);

            verify(carritoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Aplicar cupón a un carrito que ya posee un cupón lanza CuponYaAplicadoException")
        void aplicarCupon_carritoConCuponPrevio_lanzaCuponYaAplicadoException() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
            Cupon cuponExistente = CarritoTestFactory.cuponMontoFijo(new BigDecimal("100.00"));
            carrito.setCupon(cuponExistente);

            Cupon nuevoCupon = CarritoTestFactory.cuponPorcentaje(new BigDecimal("10.00"));

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("PORCENTAJE10"))
                    .thenReturn(Optional.of(nuevoCupon));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.aplicarCupon(carrito.getId(), "PORCENTAJE10", ana.getId()))
                    .as("Un carrito solo puede tener un cupón aplicado a la vez")
                    .isInstanceOf(CuponYaAplicadoException.class);

            verify(carritoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Aplicar cupón con código inexistente lanza RecursoNoEncontradoException")
        void aplicarCupon_codigoInexistente_lanzaRecursoNoEncontradoException() {
            // Given
            Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));

            when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                    .thenReturn(Optional.of(carrito));
            when(cuponesRepository.findByCodigoCupon("INEXISTENTE"))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.aplicarCupon(carrito.getId(), "INEXISTENTE", ana.getId()))
                    .as("Lanza excepción cuando el código del cupón no existe en BD")
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(carritoRepository, never()).save(any());
        }
    }
}

/*
"este test se pone rojo si alguien ______"
------------------------------------------------------------------------------------------------------------------------
- aplicarCupon_cuponVigenteYCarritoActivo_aplicaExitosamente:
  este test se pone rojo si alguien rompe el flujo correcto de asociación del cupón o la persistencia del carrito.

- aplicarCupon_cuponInactivo_lanzaCuponInvalidoException:
  este test se pone rojo si alguien quita la validación del estado ACTIVO del cupón al aplicarlo.

- aplicarCupon_cuponFuturo_lanzaCuponNoVigenteException:
  este test se pone rojo si alguien elimina la comprobación de la fecha de inicio del cupón.

- aplicarCupon_cuponExpirado_lanzaCuponExpiradoException:
  este test se pone rojo si alguien permite aplicar cupones cuya fecha de término ya transcurrió.

- aplicarCupon_carritoAbandonado_lanzaCarritoNoModificableException:
  este test se pone rojo si alguien permite modificar carritos que se encuentran marcados como ABANDONADO.

- aplicarCupon_carritoConCuponPrevio_lanzaCuponYaAplicadoException:
  este test se pone rojo si alguien permite reemplazar o superponer cupones sin antes quitar el existente.

- aplicarCupon_codigoInexistente_lanzaRecursoNoEncontradoException:
  este test se pone rojo si alguien omite validar la existencia del código de cupón solicitado.
*/