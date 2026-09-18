package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.EstadoCarrito;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.mappers.CarritoMapper;
import com.nahuel.ecommerce.mappers.ItemCarritoMapper;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.ItemCarritoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.support.CarritoTestFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de congelamiento y cálculo de precios en ítems del carrito")
class ItemCarritoPrecioTest {

 @Mock
 private ItemCarritoRepository itemCarritoRepository;

 @Mock
 private ProductoRepository productoRepository;

 @Mock
 private CarritoRepository carritoRepository;

 @Mock
 private UsuarioRepository usuarioRepository;

 @Spy
 private ItemCarritoMapper itemCarritoMapper = new ItemCarritoMapper();

 @Spy
 private CalculadoraCarrito calculadoraCarrito = new CalculadoraCarrito();

 @Spy
 private CarritoMapper carritoMapper = new CarritoMapper(itemCarritoMapper, calculadoraCarrito);

 @InjectMocks
 private ItemCarritoServiceImp itemCarritoServiceImp;

 private Usuario ana;
 private Producto productoRemera;

 @BeforeEach
 void setUp() {
  ana = new Usuario();
  ana.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  ana.setNombreUsuario("Ana");

  productoRemera = new Producto();
  productoRemera.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  productoRemera.setNombre("Remera de Algodón");
  productoRemera.setPrecioBase(new BigDecimal("1500.00"));
 }

 @Nested
 @DisplayName("Camino bueno")
 class CaminoBueno {

  @Test
  @DisplayName("Agregar ítem nuevo asigna el precio base del catálogo como precio unitario y calcula subtotal del ítem")
  void agregarItem_productoNuevo_tomaPrecioBaseDeCatalogo() {
   // Given
   Carrito carritoVacio = CarritoTestFactory.carritoVacio(ana);

   ItemRequestDto dto = new ItemRequestDto();
   dto.setProductoId(productoRemera.getId());
   dto.setCantidad(2);

   when(usuarioRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
   when(productoRepository.findById(productoRemera.getId())).thenReturn(Optional.of(productoRemera));
   when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
           .thenReturn(Optional.of(carritoVacio));
   when(itemCarritoRepository.findByCarritoIdAndProductoId(carritoVacio.getId(), productoRemera.getId()))
           .thenReturn(Optional.empty());

   // When
   CarritoDto resultado = itemCarritoServiceImp.agregarItem(dto, ana.getId());

   // Then
   ArgumentCaptor<ItemCarrito> itemCaptor = ArgumentCaptor.forClass(ItemCarrito.class);
   verify(itemCarritoRepository).save(itemCaptor.capture());

   ItemCarrito itemGuardado = itemCaptor.getValue();
   assertThat(itemGuardado.getPrecioUnitario())
           .as("El precio unitario guardado en el ítem debe ser igual al precio base del producto ($1500.00)")
           .isEqualByComparingTo(new BigDecimal("1500.00"));

   assertThat(resultado.getItems().get(0).getSubtotal())
           .as("El subtotal del ítem para 2 unidades de $1500 debe ser $3000.00")
           .isEqualByComparingTo(new BigDecimal("3000.00"));
  }

  @Test
  @DisplayName("Incrementar cantidad de un producto ya existente en carrito mantiene el precio unitario guardado previamente aunque el producto aumente en catálogo")
  void agregarItem_productoExistente_mantienePrecioUnitarioPrevio() {
   // Given
   BigDecimal precioOriginalCongelado = new BigDecimal("1000.00");
   Carrito carrito = CarritoTestFactory.carritoCon(ana, productoRemera, 1, precioOriginalCongelado);
   ItemCarrito itemExistente = carrito.getItems().iterator().next();

   // Simular aumento de precio del producto en el catálogo general
   productoRemera.setPrecioBase(new BigDecimal("2500.00"));

   ItemRequestDto dto = new ItemRequestDto();
   dto.setProductoId(productoRemera.getId());
   dto.setCantidad(2); // Se agregan 2 unidades más a la 1 existente

   when(usuarioRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
   when(productoRepository.findById(productoRemera.getId())).thenReturn(Optional.of(productoRemera));
   when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
           .thenReturn(Optional.of(carrito));
   when(itemCarritoRepository.findByCarritoIdAndProductoId(carrito.getId(), productoRemera.getId()))
           .thenReturn(Optional.of(itemExistente));

   // When
   itemCarritoServiceImp.agregarItem(dto, ana.getId());

   // Then
   assertThat(itemExistente.getPrecioUnitario())
           .as("El precio unitario del ítem en carrito NO debe actualizarse al nuevo precio del catálogo ($2500.00), sino mantener el histórico ($1000.00)")
           .isEqualByComparingTo(precioOriginalCongelado);

   assertThat(itemExistente.getCantidad())
           .as("La cantidad debe haberse incrementado a 3 unidades")
           .isEqualTo(3);

   verify(itemCarritoRepository).save(itemExistente);
  }
 }

 @Nested
 @DisplayName("Camino malo (Validaciones y Excepciones)")
 class CaminoMalo {

  @Test
  @DisplayName("Agregar ítem con ID de producto inexistente lanza RecursoNoEncontradoException y no guarda el ítem")
  void agregarItem_productoInexistente_lanzaRecursoNoEncontradoException() {
   // Given
   UUID productoIdInexistente = UUID.randomUUID();
   ItemRequestDto dto = new ItemRequestDto();
   dto.setProductoId(productoIdInexistente);
   dto.setCantidad(1);

   when(usuarioRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
   when(productoRepository.findById(productoIdInexistente)).thenReturn(Optional.empty());

   // When / Then
   assertThatThrownBy(() -> itemCarritoServiceImp.agregarItem(dto, ana.getId()))
           .as("Debe lanzar excepción cuando el producto solicitado no existe")
           .isInstanceOf(RecursoNoEncontradoException.class);

   verify(itemCarritoRepository, never()).save(any());
  }
 }
}

/*
"este test se pone rojo si alguien ______"
------------------------------------------------------------------------------------------------------------------------
- agregarItem_productoNuevo_tomaPrecioBaseDeCatalogo:
  este test se pone rojo si alguien omite mapear el precioBase del producto al precioUnitario del nuevo ItemCarrito.

- agregarItem_productoExistente_mantienePrecioUnitarioPrevio:
  este test se pone rojo si alguien sobrescribe el precioUnitario del ítem existente con el precio actual del catálogo al aumentar cantidades.

- agregarItem_productoInexistente_lanzaRecursoNoEncontradoException:
  este test se pone rojo si alguien quita la validación de existencia del producto previa al guardado del ítem.
*/