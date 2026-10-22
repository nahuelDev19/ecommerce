package com.nahuel.ecommerce.integracion;

import com.jayway.jsonpath.JsonPath;
import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.OrdenRepository;
import com.nahuel.ecommerce.repositories.PagoRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Import({PasarelaPagoFakeConfig.class, CheckoutPasarelaFakeIntegracionTest.Configuracion.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CheckoutPasarelaFakeIntegracionTest extends TestDeIntegracion {
    @Autowired private org.springframework.test.web.servlet.MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CarritoRepository carritos;
    @Autowired private ProductoRepository productos;
    @Autowired private OrdenRepository ordenes;
    @Autowired private PagoRepository pagos;
    @Autowired private PasarelaPagoFake pasarela;

    private final JwtTestHelper jwt = new JwtTestHelper();
    private Usuario comprador;

    @BeforeEach
    void preparar() {
        pasarela.reiniciar();
        jdbc.execute("TRUNCATE TABLE usuario, productos, carrito, orden, pago CASCADE");
        comprador = TestDataFactory.ana();
        comprador.setId(null);
        comprador = usuarios.saveAndFlush(comprador);
    }

    @Test
    void checkoutDevuelveIdPersistidoYUrlDelFake() throws Exception {
        Carrito carrito = nuevoCarrito();
        agregarItem(carrito, "Producto ARS", "ARS");
        carritos.saveAndFlush(carrito);

        var respuesta = mockMvc.perform(post("/api/checkout").with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.urlPago").value(PasarelaPagoFake.URL_PAGO))
                .andReturn();

        UUID ordenId = UUID.fromString(JsonPath.read(
                respuesta.getResponse().getContentAsString(), "$.ordenId"));
        Orden orden = ordenes.findByIdAndUsuarioId(ordenId, comprador.getId()).orElseThrow();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
        assertThat(orden.getTotal()).isEqualByComparingTo("200.00");
        assertThat(pagos.findByOrdenId(ordenId)).singleElement().satisfies(pago -> {
            assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
            assertThat(pago.getMonto()).isEqualByComparingTo("200.00");
            assertThat(pago.getPreferenciaId()).isEqualTo(PasarelaPagoFake.PREFERENCIA_ID);
            assertThat(pago.getUrlPago()).isEqualTo(PasarelaPagoFake.URL_PAGO);
            assertThat(pago.getPagoExternoId()).isNull();
        });
        assertThat(carritos.findById(carrito.getId()).orElseThrow().getEstadoCarrito())
                .isEqualTo(EstadoCarrito.ORDENADO);
        assertThat(pasarela.solicitudesRealizadas()).isEqualTo(1);
    }

    @Test
    void carritoVacioDevuelve400AntesDePedirPreferencia() throws Exception {
        Carrito carrito = carritos.saveAndFlush(nuevoCarrito());

        mockMvc.perform(post("/api/checkout").with(jwt.bearerToken(comprador)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("BUSINESS_RULE"));

        verificarRechazoSinCompra(carrito);
    }

    @Test
    void monedasDistintasSeRechazanAntesDePedirPreferencia() throws Exception {
        Carrito carrito = nuevoCarrito();
        agregarItem(carrito, "Producto ARS", "ARS");
        agregarItem(carrito, "Producto USD", "USD");
        carritos.saveAndFlush(carrito);

        mockMvc.perform(post("/api/checkout").with(jwt.bearerToken(comprador)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("BUSINESS_RULE"));

        verificarRechazoSinCompra(carrito);
    }

    @Test
    void detalleDevuelveSnapshotImportesYFechasGuardados() throws Exception {
        Orden orden = guardarOrden(comprador, Instant.parse("2026-01-10T12:00:00Z"), EstadoOrden.PAGADA);
        UUID productoId = orden.getItems().get(0).getProductoId();
        jdbc.update("UPDATE productos SET nombre = ?, precio_base = ? WHERE id = ?",
                "Nombre nuevo del catálogo", new BigDecimal("9999.00"), productoId);

        var respuesta = mockMvc.perform(get("/api/checkout/ordenes/{id}", orden.getId())
                        .with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(orden.getId().toString()))
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.subtotal").value(250.0))
                .andExpect(jsonPath("$.descuentoTotal").value(25.0))
                .andExpect(jsonPath("$.total").value(225.0))
                .andExpect(jsonPath("$.moneda").value("ARG"))
                .andExpect(jsonPath("$.creadoEn").value("2026-01-10T12:00:00Z"))
                .andExpect(jsonPath("$.actualizadoEn").value("2026-01-10T12:10:00Z"))
                .andExpect(jsonPath("$.pagadoEn").value("2026-01-10T12:05:00Z"))
                .andExpect(jsonPath("$.venceEn").value("2026-01-10T14:00:00Z"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        String json = respuesta.getResponse().getContentAsString();
        Map<String, Object> raiz = JsonPath.read(json, "$");
        assertThat(raiz).containsOnlyKeys("id", "estado", "items", "subtotal", "descuentoTotal",
                "total", "moneda", "creadoEn", "actualizadoEn", "pagadoEn", "venceEn");
        for (ItemOrden item : orden.getItems()) {
            String ruta = "$.items[?(@.id == '" + item.getId() + "')]";
            java.util.List<Map<String, Object>> encontrados = JsonPath.read(json, ruta);
            assertThat(encontrados).singleElement().satisfies(dto -> {
                assertThat(dto).containsOnlyKeys("id", "productoId", "nombreProducto",
                        "precioUnitario", "cantidad", "subtotal");
                assertThat(dto.get("productoId")).isEqualTo(item.getProductoId().toString());
                assertThat(dto.get("nombreProducto")).isEqualTo(item.getNombreProducto());
                assertThat(new BigDecimal(dto.get("precioUnitario").toString()))
                        .isEqualByComparingTo(item.getPrecioUnitario());
                assertThat(dto.get("cantidad")).isEqualTo(item.getCantidad());
                assertThat(new BigDecimal(dto.get("subtotal").toString()))
                        .isEqualByComparingTo(item.getSubtotal());
            });
        }
        assertThat(pasarela.solicitudesRealizadas()).isZero();
    }

    @Test
    void detallePendienteConservaFechaDePagoNula() throws Exception {
        Orden orden = guardarOrden(comprador, Instant.parse("2026-01-10T12:00:00Z"), EstadoOrden.PENDIENTE_PAGO);

        var respuesta = mockMvc.perform(get("/api/checkout/ordenes/{id}", orden.getId())
                        .with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andReturn();

        Map<String, Object> dto = JsonPath.read(respuesta.getResponse().getContentAsString(), "$");
        assertThat(dto).containsEntry("pagadoEn", null);
    }

    @Test
    void detalleAjenoDevuelve404ConControlPositivoDelPropietario() throws Exception {
        Orden orden = guardarOrden(comprador, Instant.parse("2026-01-10T12:00:00Z"), EstadoOrden.PENDIENTE_PAGO);
        Usuario otro = guardarOtroUsuario();

        mockMvc.perform(get("/api/checkout/ordenes/{id}", orden.getId()).with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orden.getId().toString()));

        var respuesta = mockMvc.perform(get("/api/checkout/ordenes/{id}", orden.getId())
                        .with(jwt.bearerToken(otro)))
                .andExpect(status().isNotFound())
                .andReturn();
        assertThat(respuesta.getResponse().getContentAsString())
                .doesNotContain("PENDIENTE_PAGO", "Producto comprado", "precioUnitario");
    }

    @Test
    void detalleInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/checkout/ordenes/{id}", UUID.randomUUID())
                        .with(jwt.bearerToken(comprador)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listadoDevuelveSoloOrdenesPropiasDeMasNuevaAMasAntigua() throws Exception {
        Orden antigua = guardarOrden(comprador, Instant.parse("2026-01-01T12:00:00Z"), EstadoOrden.PAGADA);
        Orden ajena = guardarOrden(guardarOtroUsuario(), Instant.parse("2026-01-20T12:00:00Z"), EstadoOrden.PAGADA);
        Orden nueva = guardarOrden(comprador, Instant.parse("2026-01-15T12:00:00Z"), EstadoOrden.PENDIENTE_PAGO);

        mockMvc.perform(get("/api/checkout/ordenes").with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(nueva.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(antigua.getId().toString()))
                .andExpect(jsonPath("$[0].items.length()").value(2))
                .andExpect(jsonPath("$[0].total").value(225.0));

        mockMvc.perform(get("/api/checkout/ordenes").with(jwt.bearerToken(ajena.getUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ajena.getId().toString()));
        assertThat(pasarela.solicitudesRealizadas()).isZero();
    }

    @Test
    void usuarioSinOrdenesRecibe200YListaVaciaAunqueOtroTengaCompras() throws Exception {
        guardarOrden(guardarOtroUsuario(), Instant.parse("2026-01-10T12:00:00Z"), EstadoOrden.PAGADA);

        mockMvc.perform(get("/api/checkout/ordenes").with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/checkout/ordenes", "/api/checkout/ordenes/11111111-1111-1111-1111-111111111111"})
    void consultasSinAutenticacionDevuelven401(String ruta) throws Exception {
        mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
    }

    private Usuario guardarOtroUsuario() {
        Usuario otro = TestDataFactory.beto();
        otro.setId(null);
        return usuarios.saveAndFlush(otro);
    }

    private Orden guardarOrden(Usuario usuario, Instant creadoEn, EstadoOrden estado) {
        Producto producto = productos.saveAndFlush(new Producto(
                "Catálogo " + UUID.randomUUID(), "Producto actual", new BigDecimal("150.00"),
                "ARS", true, creadoEn));
        Orden orden = new Orden();
        orden.setUsuario(usuario);
        orden.setEstado(estado);
        orden.setMoneda(TipoMoneda.ARG);
        orden.setSubtotal(new BigDecimal("250.00"));
        orden.setDescuentoTotal(new BigDecimal("25.00"));
        orden.setTotal(new BigDecimal("225.00"));
        orden.setCreadoEn(creadoEn);
        orden.setActualizadoEn(creadoEn.plusSeconds(600));
        orden.setVenceEn(creadoEn.plusSeconds(7200));
        if (estado == EstadoOrden.PAGADA) {
            orden.setPagadoEn(creadoEn.plusSeconds(300));
            orden.setPaymentId("pago-" + UUID.randomUUID());
        }
        ItemOrden primero = new ItemOrden();
        primero.setOrden(orden);
        primero.setProductoId(producto.getId());
        primero.setNombreProducto("Producto comprado");
        primero.setPrecioUnitario(new BigDecimal("100.00"));
        primero.setCantidad(2);
        orden.getItems().add(primero);
        ItemOrden segundo = new ItemOrden();
        segundo.setOrden(orden);
        segundo.setProductoId(UUID.randomUUID());
        segundo.setNombreProducto("Otro producto comprado");
        segundo.setPrecioUnitario(new BigDecimal("50.00"));
        segundo.setCantidad(1);
        orden.getItems().add(segundo);
        return ordenes.saveAndFlush(orden);
    }

    private void verificarRechazoSinCompra(Carrito carrito) {
        assertThat(ordenes.count()).isZero();
        assertThat(pagos.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM item_orden", Long.class)).isZero();
        assertThat(pasarela.solicitudesRealizadas()).isZero();
        assertThat(carritos.findById(carrito.getId()).orElseThrow().getEstadoCarrito())
                .isEqualTo(EstadoCarrito.ACTIVO);
    }

    private Carrito nuevoCarrito() {
        Carrito carrito = new Carrito();
        carrito.setUsuario(comprador);
        carrito.setCreadoEn(Instant.now());
        carrito.setActualizadoEn(carrito.getCreadoEn());
        carrito.setUltimaInteraccion(carrito.getCreadoEn());
        return carrito;
    }

    private void agregarItem(Carrito carrito, String nombre, String moneda) {
        Producto producto = productos.saveAndFlush(new Producto(
                nombre, "Producto del test", new BigDecimal("150.00"), moneda, true, Instant.now()));
        ItemCarrito item = new ItemCarrito();
        item.setCarrito(carrito);
        item.setProducto(producto);
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("100.00"));
        item.setPrecioAgregadoEn(Instant.now());
        item.setActualizadoEn(Instant.now());
        carrito.getItems().add(item);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracion {
        @Bean
        TaskScheduler taskScheduler() {
            return mock(TaskScheduler.class);
        }
    }
}
