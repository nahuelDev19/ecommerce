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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
