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
import java.time.Duration;
import java.time.Instant;
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


 private final ItemCarritoMapper itemCarritoMapper = new ItemCarritoMapper();

 private final CalculadoraCarrito calculadoraCarrito = new CalculadoraCarrito();

 private final CarritoMapper carritoMapper = new CarritoMapper(itemCarritoMapper, calculadoraCarrito);

 @InjectMocks
 private ItemCarritoServiceImp itemCarritoServiceImp;

 @InjectMocks
 private CarritoServiceImp carritoServiceImp;

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

   assertThat(itemGuardado.getPrecioAgregadoEn())
           .as("Al crear el ítem debe quedar armada la ventana de 48h")
           .isNotNull();
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

 @Test
 @DisplayName("Ítem con precio congelado VENCIDO toma el precio de lista actual")
 void itemConSnapshotVencido_tomaPrecioDeLista() {
  // Given: ítem agregado hace 49 horas con precio capturado $1000
  Carrito carrito = CarritoTestFactory.carritoCon(ana, productoRemera, 1, new BigDecimal("1000.00"));
  ItemCarrito item = carrito.getItems().iterator().next();

  // Back-dateamos el ancla. NO se espera ni se mockea el reloj:
  // como la fecha es un dato, la movemos nosotros.
  item.setPrecioAgregadoEn(Instant.now().minus(Duration.ofHours(49)));

  // El catálogo subió a $2500 mientras la ventana estaba abierta
  productoRemera.setPrecioBase(new BigDecimal("2500.00"));

  // When
  BigDecimal precio = item.precioVigente(Instant.now());

  // Then: ya no vale el snapshot, vale la lista
  assertThat(precio).isEqualByComparingTo("2500.00");
 }

 @Test
 @DisplayName("Ítem con precio congelado vigente (47h) mantiene el precio capturado")
 void itemConSnapshotVigente_mantienePrecioCapturado() {
  Carrito carrito = CarritoTestFactory.carritoCon(ana, productoRemera, 1, new BigDecimal("1000.00"));
  ItemCarrito item = carrito.getItems().iterator().next();

  item.setPrecioAgregadoEn(Instant.now().minus(Duration.ofHours(47)));
  productoRemera.setPrecioBase(new BigDecimal("2500.00"));

  assertThat(item.precioVigente(Instant.now())).isEqualByComparingTo("1000.00");
 }
 @Test
 @DisplayName("El total del carrito se recalcula cuando el snapshot vence")
 void carritoConSnapshotVencido_recalculaTotal() {
  Carrito carrito = CarritoTestFactory.carritoCon(ana, productoRemera, 2, new BigDecimal("1000.00"));
  carrito.getItems().iterator().next()
          .setPrecioAgregadoEn(Instant.now().minus(Duration.ofHours(49)));
  productoRemera.setPrecioBase(new BigDecimal("2500.00"));

  when(carritoRepository.findByUsuarioIdAndEstadoCarrito(ana.getId(), EstadoCarrito.ACTIVO))
          .thenReturn(Optional.of(carrito));

  CarritoDto resultado = carritoServiceImp.buscarCarritoActivoDelUsuarioAutenticado(ana.getId());
  var itemDto = resultado.getItems().get(0);

  // 1) Invariante: el subtotal tiene que salir del MISMO precio que se muestra.
  assertThat(itemDto.getSubtotal())
          .as("El subtotal del ítem debe ser coherente con su precio unitario")
          .isEqualByComparingTo(itemDto.getPrecioUnitario()
                  .multiply(BigDecimal.valueOf(itemDto.getCantidad())));

  // 2) Y además, el precio tiene que ser el correcto contra el catálogo.
  assertThat(itemDto.getPrecioUnitario())
          .as("Ya vencida la ventana, el ítem debe mostrar el precio de lista")
          .isEqualByComparingTo("2500.00");
  assertThat(resultado.getSubtotal()).isEqualByComparingTo("5000.00"); // 2 x 2500, no 2 x 1000
 }
 @Test
 @DisplayName("Borde exacto: a las 48h justas ya vale el precio de lista")
 void bordeDe48Horas_esExclusivo() {
  Carrito carrito = CarritoTestFactory.carritoCon(ana, productoRemera, 1, new BigDecimal("1000.00"));
  ItemCarrito item = carrito.getItems().iterator().next();

  // Usamos un instante FIJO en vez de Instant.now(). Con Instant.now() el
  // borde depende de cuántos nanosegundos tarden en correr las dos líneas,
  // y el test se vuelve frágil. Pasando el instante a mano, es determinista.
  Instant ancla = Instant.parse("2026-01-01T00:00:00Z");
  item.setPrecioAgregadoEn(ancla);
  productoRemera.setPrecioBase(new BigDecimal("2500.00"));

  Instant unMsAntes = ancla.plus(Duration.ofHours(48)).minusMillis(1);
  Instant justoDespues = ancla.plus(Duration.ofHours(48));

  assertThat(item.precioVigente(unMsAntes)).isEqualByComparingTo("1000.00");
  assertThat(item.precioVigente(justoDespues)).isEqualByComparingTo("2500.00");
 }
 @Test
 @DisplayName("Ítem sin ancla (fila previa al snapshot) respeta el precio guardado")
 void itemSinAncla_respetaPrecioGuardado() {
  Carrito carrito = CarritoTestFactory.carritoCon(ana, productoRemera, 1, new BigDecimal("1000.00"));
  ItemCarrito item = carrito.getItems().iterator().next();

  item.setPrecioAgregadoEn(null);   // fila previa a la migración V3, sin fecha
  productoRemera.setPrecioBase(new BigDecimal("2500.00"));

  // Sin ancla no se puede calcular vencimiento, así que se respeta lo guardado
  assertThat(item.precioVigente(Instant.now())).isEqualByComparingTo("1000.00");
 }

 @Test
 @DisplayName("incrementar Cantidad no renueva la ventana de 48 horas ni vambia el precio congelado")
 void agregarItem_productoExistente_noRenuevaVentana(){
  //investigar abuso de logica e implementar test
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