package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.Direccion;
import com.nahuel.ecommerce.dtos.FiltrosBusquedaProductoDto;
import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.TipoMoneda;
import com.nahuel.ecommerce.exceptions.ProductoYaRegistradoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.mappers.ProductoMapper;
import com.nahuel.ecommerce.repositories.ProductoRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias para ProductoServiceImp")
class ProductoServiceImpTest {

    @Mock
    private ProductoRepository productoRepository;

    private ProductoMapper productoMapper = new ProductoMapper();

    private ProductoServiceImp productoServiceImp;

    private UUID productoId;
    private Producto productoBase;
    private ProductoDto productoDtoBase;

    @BeforeEach
    void setUp() {
        productoServiceImp= new ProductoServiceImp(productoRepository,productoMapper);
        productoId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        productoBase = Producto.builder()
                .id(productoId)
                .nombre("Yerba Mate 500g")
                .descripcion("Yerba mate orgánica sin palo")
                .precioBase(new BigDecimal("2500.00"))
                .moneda(TipoMoneda.ARG.toString())
                .activo(true)
                .fechaCreacion(Instant.now())
                .build();

        productoDtoBase = new ProductoDto();
        productoDtoBase.setId(productoId);
        productoDtoBase.setNombre(" Yerba Mate 500g "); // Con espacios para probar trim()
        productoDtoBase.setDescripcion("Yerba mate orgánica sin palo");
        productoDtoBase.setPrecioBase(new BigDecimal("2500.00"));
        productoDtoBase.setMoneda(TipoMoneda.ARG.toString());
        productoDtoBase.setActivo(true);
    }

    @Nested
    @DisplayName("Guardar Producto (guardarProducto)")
    class GuardarProductoTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Guardar producto con nombre válido verifica la no existencia recortada y persiste la entidad")
            void guardarProducto_nombreValido_persisteYDevuelveDto() {
                // Given
                when(productoRepository.existsByNombre("Yerba Mate 500g")).thenReturn(false);
                when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
                    Producto p = invocation.getArgument(0);
                    p.setId(productoId);
                    return p;
                });

                // When
                ProductoDto resultado = productoServiceImp.guardarProducto(productoDtoBase);

                // Then
                assertThat(resultado)
                        .as("El DTO devuelto no debe ser nulo")
                        .isNotNull();

                assertThat(resultado.getId())
                        .as("El ID debe coincidir con el asignado")
                        .isEqualTo(productoId);

                assertThat(resultado.getPrecioBase())
                        .as("El precio base debe ser $2500.00")
                        .isEqualByComparingTo(new BigDecimal("2500.00"));

                ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
                verify(productoRepository).save(captor.capture());

                assertThat(captor.getValue().getFechaCreacion())
                        .as("La fecha de creación no debe ser nula")
                        .isNotNull();
            }
        }

        @Nested
        @DisplayName("Camino malo")
        class CaminoMalo {

            @Test
            @DisplayName("Guardar producto cuyo nombre recortado ya existe lanza ProductoYaRegistradoException y no guarda")
            void guardarProducto_nombreExistente_lanzaProductoYaRegistradoException() {
                // Given
                when(productoRepository.existsByNombre("Yerba Mate 500g")).thenReturn(true);

                // When / Then
                assertThatThrownBy(() -> productoServiceImp.guardarProducto(productoDtoBase))
                        .as("No se permite registrar productos con un nombre previamente existente")
                        .isInstanceOf(ProductoYaRegistradoException.class);

                verify(productoRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("Actualizar Producto por ID (actualizarPorId)")
    class ActualizarPorIdTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Actualizar producto existente actualiza campos y persiste los cambios")
            void actualizarPorId_productoExistente_actualizaCamposYGuarda() {
                // Given
                ProductoDto dtoModificado = new ProductoDto();
                dtoModificado.setNombre("Yerba Mate 1kg");
                dtoModificado.setDescripcion("Presentación familiar");
                dtoModificado.setPrecioBase(new BigDecimal("4500.00"));
                dtoModificado.setMoneda(TipoMoneda.ARG.toString());
                dtoModificado.setActivo(true);
                dtoModificado.setFechaDescontinuado(null);

                when(productoRepository.findById(productoId)).thenReturn(Optional.of(productoBase));
                when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // When
                ProductoDto resultado = productoServiceImp.actualizarPorId(productoId, dtoModificado);

                // Then
                assertThat(resultado.getNombre()).isEqualTo("Yerba Mate 1kg");
                assertThat(resultado.getPrecioBase()).isEqualByComparingTo(new BigDecimal("4500.00"));

                verify(productoRepository).save(productoBase);
            }
        }

        @Nested
        @DisplayName("Camino malo")
        class CaminoMalo {

            @Test
            @DisplayName("Actualizar producto inexistente lanza RecursoNoEncontradoException")
            void actualizarPorId_inexistente_lanzaRecursoNoEncontradoException() {
                // Given
                UUID idInexistente = UUID.randomUUID();
                when(productoRepository.findById(idInexistente)).thenReturn(Optional.empty());

                // When / Then
                assertThatThrownBy(() -> productoServiceImp.actualizarPorId(idInexistente, productoDtoBase))
                        .as("Debe fallar si el producto a actualizar no existe en la base de datos")
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(productoRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("Búsqueda con Filtros (buscarProductos)")
    class BuscarProductosTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Buscar productos con rango de precios válido ejecuta la consulta paginada")
            void buscarProductos_rangoPreciosValido_devuelvePaginaDeProductos() {
                // Given
                FiltrosBusquedaProductoDto filtros = new FiltrosBusquedaProductoDto();
                filtros.setNombre("Yerba");
                filtros.setPrecioMinimo(new BigDecimal("1000.00"));
                filtros.setPrecioMaximo(new BigDecimal("3000.00"));
                filtros.setSoloActivos(true);
                filtros.setPage(0);
                filtros.setSize(10);
                filtros.setSortBy("nombre");
                filtros.setDireccion(Direccion.ASC);

                Page<Producto> paginaSimulada = new PageImpl<>(List.of(productoBase));

                when(productoRepository.buscarConFiltros(
                        eq("Yerba"),
                        eq(new BigDecimal("1000.00")),
                        eq(new BigDecimal("3000.00")),
                        eq(true),
                        any(Pageable.class)
                )).thenReturn(paginaSimulada);

                // When
                Page<ProductoDto> resultado = productoServiceImp.buscarProductos(filtros);

                // Then
                assertThat(resultado.getContent()).hasSize(1);
                assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Yerba Mate 500g");
            }
        }

        @Nested
        @DisplayName("Camino malo")
        class CaminoMalo {

            @Test
            @DisplayName("Buscar productos con precio mínimo mayor al máximo lanza RuntimeException")
            void buscarProductos_precioMinimoMayorAlMaximo_lanzaRuntimeException() {
                // Given
                FiltrosBusquedaProductoDto filtros = new FiltrosBusquedaProductoDto();
                filtros.setPrecioMinimo(new BigDecimal("5000.00"));
                filtros.setPrecioMaximo(new BigDecimal("2000.00"));

                // When / Then
                assertThatThrownBy(() -> productoServiceImp.buscarProductos(filtros))
                        .as("El precio mínimo no puede ser mayor al precio máximo")
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("El precio mínimo no puede ser mayor al precio maximo");

                verify(productoRepository, never()).buscarConFiltros(
                        any(),
                        any(BigDecimal.class),
                        any(BigDecimal.class),
                        anyBoolean(),
                        any(Pageable.class)
                );
            }
        }
    }

    @Nested
    @DisplayName("Descontinuar Producto (descontinuarProducto)")
    class DescontinuarProductoTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Descontinuar producto marca activo en false y asigna la fecha de descontinuación")
            void descontinuarProducto_existente_marcaInactivoYGuarda() {
                // Given
                when(productoRepository.findById(productoId)).thenReturn(Optional.of(productoBase));

                // When
                productoServiceImp.descontinuarProducto(productoId);

                // Then
                assertThat(productoBase.getActivo())
                        .as("El producto debe marcarse como inactivo")
                        .isFalse();

                assertThat(productoBase.getFechaDescontinuado())
                        .as("Debe asignarse la fecha actual de descontinuación")
                        .isNotNull();

                verify(productoRepository).save(productoBase);
            }
        }

        @Nested
        @DisplayName("Camino malo")
        class CaminoMalo {

            @Test
            @DisplayName("Descontinuar producto inexistente lanza RecursoNoEncontradoException")
            void descontinuarProducto_inexistente_lanzaRecursoNoEncontradoException() {
                // Given
                UUID idInexistente = UUID.randomUUID();
                when(productoRepository.findById(idInexistente)).thenReturn(Optional.empty());

                // When / Then
                assertThatThrownBy(() -> productoServiceImp.descontinuarProducto(idInexistente))
                        .as("Debe fallar al intentar descontinuar un producto inexistente")
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(productoRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("Eliminar por ID (eliminarPorId)")
    class EliminarPorIdTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Eliminar por ID delega la llamada directamente al repositorio deleteById")
            void eliminarPorId_ejecutaDeleteEnRepositorio() {
                // When
                productoServiceImp.eliminarPorId(productoId);

                // Then
                verify(productoRepository).deleteById(productoId);
            }
        }
    }
}

/*
"este test se pone rojo si alguien ______"
------------------------------------------------------------------------------------------------------------------------
- guardarProducto_nombreValido_persisteYDevuelveDto:
  este test se pone rojo si alguien remueve la llamada a save() o no recorta el nombre del producto al guardarlo.

- guardarProducto_nombreExistente_lanzaProductoYaRegistradoException:
  este test se pone rojo si alguien deshabilita la comprobación por existsByNombre antes de crear un producto.

- actualizarPorId_productoExistente_actualizaCamposYGuarda:
  este test se pone rojo si alguien elimina la actualización de propiedades o el guardado en el método actualizarPorId.

- actualizarPorId_inexistente_lanzaRecursoNoEncontradoException:
  este test se pone rojo si alguien remueve la validación de existencia del producto previo a su actualización.

- buscarProductos_rangoPreciosValido_devuelvePaginaDeProductos:
  este test se pone rojo si alguien altera la construcción del objeto Pageable o el mapeo de filtros hacia la consulta del repositorio.

- buscarProductos_precioMinimoMayorAlMaximo_lanzaRuntimeException:
  este test se pone rojo si alguien elimina la validación que impide que el precio mínimo sea superior al máximo.

- descontinuarProducto_existente_marcaInactivoYGuarda:
  este test se pone rojo si alguien omite cambiar el flag activo a false o la fechaDescontinuado antes de guardar.

- descontinuarProducto_inexistente_lanzaRecursoNoEncontradoException:
  este test se pone rojo si alguien intenta descontinuar un producto sin comprobar previamente su existencia.

- eliminarPorId_ejecutaDeleteEnRepositorio:
  este test se pone rojo si alguien remueve la invocación a deleteById en el servicio.
*/