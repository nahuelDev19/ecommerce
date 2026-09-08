package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.CuponesRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de cálculo de totales, subtotales y descuentos en Carrito")
class CarritoTotalesTest {

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuponesRepository cuponesRepository;

    @Mock
    private ItemCarritoRepository itemCarritoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CarritoService carritoService;

    @InjectMocks
    private CarritoServiceImp carritoServiceImp;

    @InjectMocks
    private CuponServiceImp cuponServiceImp;

    @InjectMocks
    private ItemCarritoServiceImp itemCarritoServiceImp;

    private Usuario ana;
    private Producto productoMil;
    private Producto productoQuinientos;

    @BeforeEach
    void setUp() {
        ana = new Usuario();
        ana.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        ana.setNombreUsuario("Ana");

        productoMil = new Producto();
        productoMil.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        productoMil.setNombre("Producto $1000");
        productoMil.setPrecioBase(new BigDecimal("1000.00"));

        productoQuinientos = new Producto();
        productoQuinientos.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        productoQuinientos.setNombre("Producto $500");
        productoQuinientos.setPrecioBase(new BigDecimal("500.00"));
    }

    @Nested
    @DisplayName("Cálculo de totales en CarritoServiceImp")
    class CarritoServiceTotalesTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Obtener carrito activo de Ana con 1 producto de $1000 y cupón del 10% retorna subtotal 1000, descuento 100 y total 900")
            void buscarCarritoActivo_conCuponPorcentaje_calculaTotalesCorrectamente() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
                Cupon cuponDiezPorCiento = CarritoTestFactory.cuponPorcentaje(new BigDecimal("10.00"));
                carrito.setCupon(cuponDiezPorCiento);

                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));

                // When
                CarritoDto resultado = carritoServiceImp.buscarCarritoActivoDelUsuarioAutenticado(ana.getId());

                // Then
                assertThat(resultado.getSubtotal())
                        .as("El subtotal debe ser $1000.00")
                        .isEqualByComparingTo(new BigDecimal("1000.00"));

                assertThat(resultado.getDescuentoTotal())
                        .as("El descuento del 10% debe ser $100.00")
                        .isEqualByComparingTo(new BigDecimal("100.00"));

                assertThat(resultado.getTotal())
                        .as("El total final debe ser $900.00")
                        .isEqualByComparingTo(new BigDecimal("900.00"));
            }

            @Test
            @DisplayName("Carrito sin cupón aplicado retorna descuento en 0.00 y total igual al subtotal")
            void buscarCarritoActivo_sinCupon_descuentoCeroYTotalIgualSubtotal() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));

                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));

                // When
                CarritoDto resultado = carritoServiceImp.buscarCarritoActivoDelUsuarioAutenticado(ana.getId());

                // Then
                assertThat(resultado.getSubtotal())
                        .as("El subtotal debe ser $1000.00")
                        .isEqualByComparingTo(new BigDecimal("1000.00"));

                assertThat(resultado.getDescuentoTotal())
                        .as("Sin cupón, el descuento debe ser $0.00 y nunca null")
                        .isNotNull()
                        .isEqualByComparingTo(BigDecimal.ZERO);

                assertThat(resultado.getTotal())
                        .as("El total debe ser exactamente igual al subtotal")
                        .isEqualByComparingTo(new BigDecimal("1000.00"));
            }

            @Test
            @DisplayName("Carrito con dos ítems ($1000 x2 y $500 x1) calcula subtotal en $2500")
            void buscarCarritoActivo_variosItems_calculaSubtotalAcumulado() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 2, new BigDecimal("1000.00"));

                ItemCarrito item2 = new ItemCarrito();
                item2.setId(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002"));
                item2.setCarrito(carrito);
                item2.setProducto(productoQuinientos);
                item2.setCantidad(1);
                item2.setPrecioUnitario(new BigDecimal("500.00"));

                carrito.getItems().add(item2);

                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));

                // When
                CarritoDto resultado = carritoServiceImp.buscarCarritoActivoDelUsuarioAutenticado(ana.getId());

                // Then
                assertThat(resultado.getSubtotal())
                        .as("El subtotal para 2x1000 + 1x500 debe ser $2500.00")
                        .isEqualByComparingTo(new BigDecimal("2500.00"));

                assertThat(resultado.getTotal())
                        .as("Sin cupón el total debe ser $2500.00")
                        .isEqualByComparingTo(new BigDecimal("2500.00"));
            }
        }
    }

    @Nested
    @DisplayName("Cálculo de totales en CuponServiceImp")
    class CuponServiceTotalesTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Aplicar cupón del 10% sobre carrito de $1000 retorna subtotal 1000, descuento 100 y total 900")
            void aplicarCupon_porcentaje_devuelveMismosTotalesQueConsulta() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
                Cupon cuponDiezPorCiento = CarritoTestFactory.cuponPorcentaje(new BigDecimal("10.00"));

                when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("PORCENTAJE10"))
                        .thenReturn(Optional.of(cuponDiezPorCiento));
                when(carritoRepository.save(any(Carrito.class)))
                        .thenReturn(carrito);

                // When
                CarritoDto resultado = cuponServiceImp.aplicarCupon(carrito.getId(), "PORCENTAJE10", ana.getId());

                // Then
                assertThat(resultado.getSubtotal())
                        .as("El subtotal debe ser $1000.00")
                        .isEqualByComparingTo(new BigDecimal("1000.00"));

                assertThat(resultado.getDescuentoTotal())
                        .as("El descuento debe ser $100.00")
                        .isEqualByComparingTo(new BigDecimal("100.00"));

                assertThat(resultado.getTotal())
                        .as("El total final debe ser $900.00")
                        .isEqualByComparingTo(new BigDecimal("900.00"));
            }

            @Test
            @DisplayName("Aplicar cupón de monto fijo $300 sobre $1000 da total $700")
            void aplicarCupon_montoFijoMenorAlSubtotal_descuentaMontoExacto() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
                Cupon cuponMontoFijo = CarritoTestFactory.cuponMontoFijo(new BigDecimal("300.00"));

                when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("FIXED1000"))
                        .thenReturn(Optional.of(cuponMontoFijo));
                when(carritoRepository.save(any(Carrito.class)))
                        .thenReturn(carrito);

                // When
                CarritoDto resultado = cuponServiceImp.aplicarCupon(carrito.getId(), "FIXED1000", ana.getId());

                // Then
                assertThat(resultado.getSubtotal())
                        .as("El subtotal debe ser $1000.00")
                        .isEqualByComparingTo(new BigDecimal("1000.00"));

                assertThat(resultado.getDescuentoTotal())
                        .as("El descuento debe ser exactamente $300.00")
                        .isEqualByComparingTo(new BigDecimal("300.00"));

                assertThat(resultado.getTotal())
                        .as("El total debe ser $700.00")
                        .isEqualByComparingTo(new BigDecimal("700.00"));
            }

            @Test
            @DisplayName("Aplicar cupón de monto fijo ($1500) mayor al subtotal ($1000) limita el descuento al subtotal y no produce total negativo")
            void aplicarCupon_montoFijoMayorAlSubtotal_totalNoEsNegativo() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
                Cupon cuponMontoGrande = CarritoTestFactory.cuponMontoFijo(new BigDecimal("1500.00"));

                when(carritoRepository.findByIdAndUsuarioId(carrito.getId(), ana.getId()))
                        .thenReturn(Optional.of(carrito));
                when(cuponesRepository.findByCodigoCupon("FIXED1000"))
                        .thenReturn(Optional.of(cuponMontoGrande));
                when(carritoRepository.save(any(Carrito.class)))
                        .thenReturn(carrito);

                // When
                CarritoDto resultado = cuponServiceImp.aplicarCupon(carrito.getId(), "FIXED1000", ana.getId());

                // Then
                assertThat(resultado.getDescuentoTotal())
                        .as("El descuento no puede superar el subtotal de $1000.00")
                        .isEqualByComparingTo(new BigDecimal("1000.00"));

                assertThat(resultado.getTotal())
                        .as("El total no puede ser negativo, el mínimo es $0.00")
                        .isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }

    @Nested
    @DisplayName("Cálculo de totales en ItemCarritoServiceImp al modificar ítems")
    class ItemCarritoServiceTotalesTests {

        @Nested
        @DisplayName("Camino bueno")
        class CaminoBueno {

            @Test
            @DisplayName("Agregar un item a un carrito con cupón del 10% retorna el descuento aplicado (NO nulo) y el total con descuento restado")
            void agregarItem_enCarritoConCupon_mantieneCalculoDeDescuentoYTotal() {
                // Given
                Carrito carrito = CarritoTestFactory.carritoCon(ana, productoMil, 1, new BigDecimal("1000.00"));
                Cupon cuponDiezPorCiento = CarritoTestFactory.cuponPorcentaje(new BigDecimal("10.00"));
                carrito.setCupon(cuponDiezPorCiento);

                ItemCarrito itemExistente = carrito.getItems().iterator().next();

                ItemRequestDto dto = new ItemRequestDto();
                dto.setProductoId(productoMil.getId());
                dto.setCantidad(1);

                when(usuarioRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
                when(productoRepository.findById(productoMil.getId())).thenReturn(Optional.of(productoMil));
                when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
                        .thenReturn(Optional.of(carrito));
                when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), productoMil.getId()))
                        .thenReturn(Optional.of(itemExistente));

                // When
                CarritoDto resultado = itemCarritoServiceImp.agregarItem(dto, ana.getId());

                // Then
                assertThat(resultado.getSubtotal())
                        .as("El subtotal acumulado para 2 unidades de $1000 debe ser $2000.00")
                        .isEqualByComparingTo(new BigDecimal("2000.00"));

                assertThat(resultado.getDescuentoTotal())
                        .as("El descuento del 10% no debe ser nulo y debe corresponder a $200.00")
                        .isNotNull()
                        .isEqualByComparingTo(new BigDecimal("200.00"));

                assertThat(resultado.getTotal())
                        .as("El total con el descuento restado debe ser $1800.00")
                        .isEqualByComparingTo(new BigDecimal("1800.00"));
            }
        }
    }
}

/*
"este test se pone rojo si alguien ______"
------------------------------------------------------------------------------------------------------------------------
- buscarCarritoActivo_conCuponPorcentaje_calculaTotalesCorrectamente:
  este test se pone rojo si alguien altera la fórmula de cálculo de subtotal, descuento o total para cupones porcentuales en CarritoServiceImp.

- buscarCarritoActivo_sinCupon_descuentoCeroYTotalIgualSubtotal:
  este test se pone rojo si alguien cambia la lógica para que el descuento sin cupón devuelva null o un valor distinto a cero.

- buscarCarritoActivo_variosItems_calculaSubtotalAcumulado:
  este test se pone rojo si alguien rompe la acumulación de precios unitarios multiplicados por cantidad en el subtotal.

- aplicarCupon_porcentaje_devuelveMismosTotalesQueConsulta:
  este test se pone rojo si alguien modifica el cálculo de totales en la respuesta del método aplicarCupon en CuponServiceImp.

- aplicarCupon_montoFijoMenorAlSubtotal_descuentaMontoExacto:
  este test se pone rojo si alguien ignora el valor del descuento de tipo MONTOFIJO al calcular los totales del DTO.

- aplicarCupon_montoFijoMayorAlSubtotal_totalNoEsNegativo:
  este test se pone rojo si alguien elimina la protección contra totales negativos en cupones de monto fijo superiores al subtotal.

- agregarItem_enCarritoConCupon_mantieneCalculoDeDescuentoYTotal:
  este test se pone rojo si alguien omite calcular o incluir el descuento acumulado y el total neto al agregar ítems desde ItemCarritoServiceImp.
*/