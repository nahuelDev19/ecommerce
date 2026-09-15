package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.dtos.CuponDtoResponse;
import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.entitys.EstadoCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;
import com.nahuel.ecommerce.entitys.TipoMoneda;
import com.nahuel.ecommerce.exceptions.CuponYaExisteException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias para la creación de cupones")
class CuponCreacionTest {

    @Mock
    private CuponesRepository cuponesRepository;

    @InjectMocks
    private CuponServiceImp cuponServiceImp;

    private CuponDto cuponDtoBase;
    private Instant ahora;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();

        cuponDtoBase = new CuponDto();
        cuponDtoBase.setCodigoCupon("verano2026");
        cuponDtoBase.setDescripcion("Cupón de temporada de verano");
        cuponDtoBase.setAlcanceCupon(AlcanceCupon.POR_CARRITO);
        cuponDtoBase.setTipoDescuento(TipoDescuento.PORCENTAJE);
        cuponDtoBase.setValorDescuento(new BigDecimal("15.00"));
        cuponDtoBase.setMoneda(TipoMoneda.ARG);
        cuponDtoBase.setMontoMinimoCarrito(BigDecimal.ZERO);
        cuponDtoBase.setLimiteUsoPorUsuario(1);
        cuponDtoBase.setIniciadoEn(ahora);
        cuponDtoBase.setTerminadoEn(ahora.plus(Duration.ofDays(10)));
    }

    @Nested
    @DisplayName("Camino bueno")
    class CaminoBueno {

        @Test
        @DisplayName("Crear cupón con datos válidos convierte el código a mayúsculas, establece estado ACTIVO y guarda en repositorio")
        void crearCupon_datosValidos_guardaEnMayusculasYDevuelveDto() {
            // Given
            when(cuponesRepository.findByCodigoCupon("VERANO2026")).thenReturn(Optional.empty());
            when(cuponesRepository.save(any(Cupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CuponDtoResponse respuesta = cuponServiceImp.crearCupon(cuponDtoBase);

            // Then
            assertThat(respuesta)
                    .as("La respuesta no debe ser nula")
                    .isNotNull();

            assertThat(respuesta.getCodigoCupon())
                    .as("El código debe haberse normalizado a mayúsculas")
                    .isEqualTo("VERANO2026");

            assertThat(respuesta.getEstadoCupon())
                    .as("El nuevo cupón debe iniciar en estado ACTIVO")
                    .isEqualTo(EstadoCupon.ACTIVO);

            ArgumentCaptor<Cupon> cuponCaptor = ArgumentCaptor.forClass(Cupon.class);
            verify(cuponesRepository).save(cuponCaptor.capture());
            assertThat(cuponCaptor.getValue().getCodigoCupon())
                    .isEqualTo("VERANO2026");
        }

        @Test
        @DisplayName("Crear cupón sin fecha de inicio explícita asigna la fecha y hora actual")
        void crearCupon_sinIniciadoEn_asignaInstantActual() {
            // Given
            cuponDtoBase.setIniciadoEn(null);

            when(cuponesRepository.findByCodigoCupon("VERANO2026")).thenReturn(Optional.empty());
            when(cuponesRepository.save(any(Cupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CuponDtoResponse respuesta = cuponServiceImp.crearCupon(cuponDtoBase);

            // Then
            assertThat(respuesta.getIniciadoEn())
                    .as("Debe asignar una fecha de inicio por defecto no nula")
                    .isNotNull();

            verify(cuponesRepository).save(any(Cupon.class));
        }
    }

    @Nested
    @DisplayName("Camino malo (Validaciones y Reglas de Negocio)")
    class CaminoMalo {

        @Test
        @DisplayName("Crear cupón con fecha de término anterior a la de inicio lanza ReglaNegocioException")
        void crearCupon_fechaTerminoAnteriorAInicio_lanzaReglaNegocioException() {
            // Given
            cuponDtoBase.setIniciadoEn(ahora);
            cuponDtoBase.setTerminadoEn(ahora.minus(Duration.ofDays(1)));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.crearCupon(cuponDtoBase))
                    .as("No se puede crear un cupón cuya fecha de fin sea anterior a la de inicio")
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Terminado en no puede ser anterior a IniciadoEn");

            verify(cuponesRepository, never()).save(any());
        }

        @Test
        @DisplayName("Crear cupón de porcentaje con descuento mayor a 100% lanza ReglaNegocioException")
        void crearCupon_porcentajeMayorA100_lanzaReglaNegocioException() {
            // Given
            cuponDtoBase.setTipoDescuento(TipoDescuento.PORCENTAJE);
            cuponDtoBase.setValorDescuento(new BigDecimal("100.00"));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.crearCupon(cuponDtoBase))
                    .as("El porcentaje de descuento no debe ser mayor a 100%")
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("El porcentaje del cupon no debe ser mayor o igual a 100%");

            verify(cuponesRepository, never()).save(any());
        }

        @Test
        @DisplayName("Crear cupón con un código que ya existe en el repositorio lanza CuponYaExisteException")
        void crearCupon_codigoExistente_lanzaCuponYaExisteException() {
            // Given
            Cupon cuponExistente = new Cupon();
            cuponExistente.setCodigoCupon("VERANO2026");

            when(cuponesRepository.findByCodigoCupon("VERANO2026"))
                    .thenReturn(Optional.of(cuponExistente));

            // When / Then
            assertThatThrownBy(() -> cuponServiceImp.crearCupon(cuponDtoBase))
                    .as("No se permite duplicar códigos de cupón")
                    .isInstanceOf(CuponYaExisteException.class);

            verify(cuponesRepository, never()).save(any());
        }
    }
}

/*
"este test se pone rojo si alguien ______"
------------------------------------------------------------------------------------------------------------------------
- crearCupon_datosValidos_guardaEnMayusculasYDevuelveDto:
  este test se pone rojo si alguien elimina la conversión a mayúsculas del código o el guardado del nuevo cupón en el repositorio.

- crearCupon_sinIniciadoEn_asignaInstantActual:
  este test se pone rojo si alguien quita el fallback que asigna Instant.now() cuando iniciadoEn viene nulo.

- crearCupon_fechaTerminoAnteriorAInicio_lanzaReglaNegocioException:
  este test se pone rojo si alguien quita la comprobación de coherencia entre las fechas de inicio y fin.

- crearCupon_porcentajeMayorA99_lanzaReglaNegocioException:
  este test se pone rojo si alguien permite crear cupones porcentuales con un valor del 100% o superior.

- crearCupon_codigoExistente_lanzaCuponYaExisteException:
  este test se pone rojo si alguien ignora la verificación de unicidad del código de cupón antes de intentar crearlo.
*/