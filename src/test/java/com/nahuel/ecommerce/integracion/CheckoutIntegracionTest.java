package com.nahuel.ecommerce.integracion;

import com.nahuel.ecommerce.checkout.CheckoutService;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.repositories.*;
import com.nahuel.ecommerce.support.JwtTestHelper;
import com.nahuel.ecommerce.support.TestDataFactory;
import com.nahuel.ecommerce.support.TestDeIntegracion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints y filtros reales + PostgreSQL temporal de TestDeIntegracion.
 * Solo la frontera HTTP con Mercado Pago se simula: no se mockean servicios
 * ni repositorios. No usar @Transactional en esta clase: comprobamos commits
 * reales del servicio, incluso cuando el controller devuelve 200 ante errores.
 * Como los otros tests de integración, usa create-drop y Flyway deshabilitado;
 * estos casos verifican el flujo, no la ejecución de los scripts de migración.
 */
@AutoConfigureMockMvc
@Import(CheckoutIntegracionTest.Configuracion.class)
@TestPropertySource(properties = {
        "mercadopago.api-url=https://mercadopago.test.invalid",
        "mercadopago.access-token=token-falso-exclusivo-checkout-test",
        "mercadopago.webhook-secret=secreto-falso-exclusivo-checkout-test",
        "mercadopago.notification-url=https://backend.test.invalid/api/webhooks/mercadopago",
        "checkout.vigencia-orden=PT2H",
        "ADMIN_EMAIL=admin-checkout@test.invalid",
        "PASS_ADMIN="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CheckoutIntegracionTest extends TestDeIntegracion {

    private static final String API = "https://mercadopago.test.invalid";
    private static final String URL_PAGO = "https://checkout.test.invalid/pagar";
    private static final String SECRETO_WEBHOOK = "secreto-falso-exclusivo-checkout-test";
    private static final String ID_PAGO = "987654321";
    private static final String TIMESTAMP_WEBHOOK = Long.toString(Instant.now().getEpochSecond());
    private static final Instant FECHA_APROBACION = Instant.parse("2026-01-10T12:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProductoRepository productos;
    @Autowired private CarritoRepository carritos;
    @Autowired private OrdenRepository ordenes;
    @Autowired private PagoRepository pagos;
    @Autowired private CheckoutService checkoutService;
    @Autowired private MockRestServiceServer mercadoPago;

    private final JwtTestHelper jwt = new JwtTestHelper();
    private Usuario comprador;

    @BeforeEach
    void prepararBaseTemporal() {
        mercadoPago.reset();
        // Solo afecta al PostgreSQL descartable de @ServiceConnection.
        // CASCADE limpia dependencias por FK sin depender del orden de borrado.
        jdbc.execute("TRUNCATE TABLE usuario, productos, carrito, orden, pago CASCADE");
        comprador = guardarUsuario(TestDataFactory.ana());
    }

    @AfterEach
    void verificarConsultasHttp() {
        // También detecta si el controller ocultó un error antes de llamar a la API.
        mercadoPago.verify();
    }

    @Test
    @DisplayName("24.1 - Checkout copia cantidades y resuelve precios vigentes, vencidos y sin fecha")
    void carritoConProductosCreaOrdenConSnapshot() throws Exception {
        // PREPARACIÓN: precios del catálogo distintos del snapshot del carrito.
        // Dentro de 48 h: conserva 100. Pasadas 48 h: usa 200 del catálogo.
        // Sin fecha: conserva 40, según la regla actual de ItemCarrito.
        Instant ahora = Instant.now();
        Carrito carrito = carritoActivo();
        agregarItem(carrito, "Vigente", "150.00", "100.00", 2,
                ahora.minus(Duration.ofHours(1)));
        agregarItem(carrito, "Vencido", "200.00", "80.00", 3,
                ahora.minus(Duration.ofHours(49)));
        agregarItem(carrito, "Sin fecha", "90.00", "40.00", 4, null);
        carritos.saveAndFlush(carrito);

        mercadoPago.expect(once(), requestTo(API + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-falso-exclusivo-checkout-test"))
                .andExpect(jsonPath("$.items[0].unit_price").value(960.0))
                .andExpect(jsonPath("$.items[0].currency_id").value("ARS"))
                // La respuesta simulada ahora incluye los dos datos de la preferencia.
                .andRespond(withSuccess("{\"id\":\"preferencia-test-001\",\"init_point\":\"" + URL_PAGO + "\"}",
                        MediaType.APPLICATION_JSON));

        // EJECUCIÓN: JWT real de prueba, sin desactivar filtros ni reemplazar principal.
        var respuestaCheckout = mockMvc.perform(post("/api/checkout").with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.urlPago").value(URL_PAGO))
                .andReturn();

        // VERIFICACIÓN: leemos lo persistido, no el objeto armado por el test.
        assertThat(cantidad("orden")).isEqualTo(1);
        Orden orden = ordenes.findAll().get(0);
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(
                respuestaCheckout.getResponse().getContentAsString(), "$.ordenId"))
                .isEqualTo(orden.getId().toString());
        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
        assertThat(orden.getUsuario().getId()).isEqualTo(comprador.getId());
        assertThat(orden.getSubtotal()).isEqualByComparingTo("960.00");
        assertThat(orden.getTotal()).isEqualByComparingTo("960.00");
        assertThat(orden.getDescuentoTotal()).isEqualByComparingTo("0");
        assertThat(orden.getMoneda()).isEqualTo(TipoMoneda.ARG);
        assertThat(Duration.between(orden.getCreadoEn(), orden.getVenceEn()))
                .isEqualTo(Duration.ofHours(2));
        // El enlace no acredita un cobro: se guarda un único intento pendiente
        // con la preferencia y la URL, sin inventar un ID de pago externo.
        assertThat(cantidad("pago")).isEqualTo(1);
        var intentos = pagos.findByOrdenId(orden.getId());
        assertThat(intentos).hasSize(1);
        Pago pago = intentos.get(0);
        assertThat(pago.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.getMonto()).isEqualByComparingTo(orden.getTotal());
        assertThat(pago.getMoneda()).isEqualTo(orden.getMoneda());
        assertThat(pago.getPreferenciaId()).isEqualTo("preferencia-test-001");
        assertThat(pago.getUrlPago()).isEqualTo(URL_PAGO);
        assertThat(pago.getPagoExternoId()).isNull();
        assertThat(pago.getCreadoEn()).isEqualTo(orden.getCreadoEn());
        assertThat(pago.getActualizadoEn()).isAfterOrEqualTo(pago.getCreadoEn());
        verificarItemsCopiados(orden.getId(), carrito);

        Carrito consumido = carritos.findById(carrito.getId()).orElseThrow();
        assertThat(consumido.getEstadoCarrito()).isEqualTo(EstadoCarrito.ORDENADO);
        assertThat(consumido.getActualizadoEn()).isAfterOrEqualTo(consumido.getCreadoEn());
        assertThat(cantidad("item_carrito")).isEqualTo(3);

        // Otro clic no crea otra orden ni solicita una segunda preferencia.
        mockMvc.perform(post("/api/checkout").with(jwt.bearerToken(comprador)))
                .andExpect(status().isNotFound());
        assertThat(cantidad("orden")).isEqualTo(1);
        assertThat(cantidad("pago")).isEqualTo(1);

        // Cambiar luego el catálogo no debe alterar las compras ya registradas.
        jdbc.update("UPDATE productos SET precio_base = 9999, nombre = nombre || '-nuevo'");
        verificarItemsCopiados(orden.getId(), carrito);
    }

    @Test
    @DisplayName("Fallo de pasarela revierte la orden, sus ítems y el pago pendiente")
    void falloDePasarelaNoDejaCompraParcial() {
        Carrito carrito = carritoActivo();
        agregarItem(carrito, "Producto rollback", "100.00", "100.00", 1, Instant.now());
        carritos.saveAndFlush(carrito);
        mercadoPago.expect(once(), requestTo(API + "/checkout/preferences"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        // Se llama al bean de Spring: atraviesa el proxy transaccional real.
        // Sin @Transactional en el test, el rollback lo debe hacer el servicio.
        assertThatThrownBy(() -> checkoutService.iniciarCheckout(comprador.getId()))
                .isInstanceOf(RestClientResponseException.class);

        assertThat(cantidad("orden")).isZero();
        assertThat(cantidad("item_orden")).isZero();
        assertThat(cantidad("pago")).isZero();
        assertThat(carritos.findById(carrito.getId()).orElseThrow().getEstadoCarrito())
                .isEqualTo(EstadoCarrito.ACTIVO);
        assertThat(cantidad("item_carrito")).isEqualTo(1);
    }

    @Test
    @DisplayName("Dos checkouts simultáneos del mismo carrito crean una sola compra")
    void checkoutsSimultaneosNoDuplicanLaCompra() throws Exception {
        Carrito carrito = carritoActivo();
        agregarItem(carrito, "Producto concurrente", "100.00", "100.00", 1, Instant.now());
        carritos.saveAndFlush(carrito);
        UUID usuarioId = comprador.getId();
        CountDownLatch primeraEnPasarela = new CountDownLatch(1);
        CountDownLatch liberarPasarela = new CountDownLatch(1);
        mercadoPago.expect(once(), requestTo(API + "/checkout/preferences"))
                .andRespond(request -> {
                    primeraEnPasarela.countDown();
                    try {
                        if (!liberarPasarela.await(20, TimeUnit.SECONDS)) {
                            throw new java.io.IOException("No se liberó la pasarela simulada");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new java.io.IOException(e);
                    }
                    return withSuccess("{\"id\":\"preferencia-concurrente\",\"init_point\":\""
                            + URL_PAGO + "\"}", MediaType.APPLICATION_JSON).createResponse(request);
                });

        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            var primero = ejecutor.submit(() -> checkoutService.iniciarCheckout(usuarioId));
            assertThat(primeraEnPasarela.await(15, TimeUnit.SECONDS)).isTrue();
            var segundo = ejecutor.submit(() -> {
                try {
                    checkoutService.iniciarCheckout(usuarioId);
                    return "CREADO";
                } catch (RecursoNoEncontradoException e) {
                    return "SIN_CARRITO_ACTIVO";
                }
            });

            // No basta lanzar dos hilos: comprobamos que PostgreSQL realmente
            // tiene el segundo checkout esperando un bloqueo sobre carrito.
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                    assertThat(jdbc.queryForObject("""
                            SELECT count(*) FROM pg_stat_activity
                            WHERE datname = current_database()
                              AND wait_event_type = 'Lock'
                              AND query ILIKE '%carrito%'
                            """, Long.class)).isGreaterThan(0));
            liberarPasarela.countDown();
            var creado = primero.get(15, TimeUnit.SECONDS);
            assertThat(creado.urlPago()).isEqualTo(URL_PAGO);
            assertThat(ordenes.findById(creado.ordenId())).isPresent();
            assertThat(segundo.get(15, TimeUnit.SECONDS)).isEqualTo("SIN_CARRITO_ACTIVO");
        } finally {
            liberarPasarela.countDown();
            ejecutor.shutdownNow();
            assertThat(ejecutor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(cantidad("orden")).isEqualTo(1);
        assertThat(cantidad("pago")).isEqualTo(1);
        assertThat(cantidad("item_orden")).isEqualTo(1);
        assertThat(carritos.findById(carrito.getId()).orElseThrow().getEstadoCarrito())
                .isEqualTo(EstadoCarrito.ORDENADO);
    }

    @Test
    @DisplayName("24.2 - Carrito vacío devuelve 400 y no crea órdenes ni pagos")
    void carritoVacioNoCreaOrden() throws Exception {
        // PREPARACIÓN: el carrito existe y pertenece al usuario autenticado, pero no tiene ítems.
        carritos.saveAndFlush(carritoActivo());
        // No configuramos respuestas HTTP: cualquier llamada a Mercado Pago fallaría el test.

        // EJECUCIÓN: usamos el endpoint y el JWT del propietario.
        mockMvc.perform(post("/api/checkout").with(jwt.bearerToken(comprador)))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.title").value("BUSINESS_RULE"));

        // VERIFICACIÓN: rechazar debe dejar la base sin compras parciales.
        assertThat(cantidad("orden")).isZero();
        assertThat(cantidad("item_orden")).isZero();
        assertThat(cantidad("pago")).isZero();
    }

    @Test
    @DisplayName("24.3 - Webhook firmado de pago aprobado registra Pago y marca la orden PAGADA")
    void pagoAprobadoActualizaOrden() throws Exception {
        // Conservamos el caso existente y lo adaptamos al pago que crea checkout.
        // Cada consulta ahora obtiene el pago y su orden comercial.
        Orden orden = ordenPendiente();
        Pago pendiente = guardarPagoPendiente(orden);
        esperarPagoAprobado(orden, 2);

        // EJECUCIÓN: Mercado Pago no envía JWT; sí enviamos una firma HMAC válida.
        enviarWebhookFirmado("request-aprobado");

        // VERIFICACIÓN: el 200 no es suficiente; comprobamos ambos commits en PostgreSQL.
        verificarPagoAprobado(orden);
        assertThat(pagos.findByPagoExternoId(ID_PAGO).orElseThrow().getId())
                .isEqualTo(pendiente.getId());
        assertThat(cantidad("orden")).isEqualTo(1);
        assertThat(cantidad("item_orden")).isEqualTo(1);
    }

    @Test
    @DisplayName("24.4 - El mismo webhook dos veces conserva pago, ítems y fecha de aprobación")
    void webhookRepetidoNoDuplicaEfectos() throws Exception {
        // PREPARACIÓN: mismo pago y respuesta de la API para los dos avisos.
        Orden orden = ordenPendiente();
        Pago pendiente = guardarPagoPendiente(orden);
        esperarPagoAprobado(orden, 4);

        // EJECUCIÓN 1: registramos las identidades y fechas ya confirmadas.
        enviarWebhookFirmado("request-repetido");
        verificarPagoAprobado(orden);
        Orden primera = ordenes.findById(orden.getId()).orElseThrow();
        UUID idPago = jdbc.queryForObject("SELECT id FROM pago", UUID.class);
        assertThat(idPago).isEqualTo(pendiente.getId());
        var itemsOriginales = jdbc.queryForList("SELECT * FROM item_orden ORDER BY id");

        // EJECUCIÓN 2: mismo cuerpo, data.id, request-id, timestamp y firma.
        enviarWebhookFirmado("request-repetido");

        // VERIFICACIÓN: no basta contar registros; comprobamos que no se reemplazaron.
        verificarPagoAprobado(orden);
        assertThat(cantidad("orden")).isEqualTo(1);
        assertThat(cantidad("item_orden")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM pago", UUID.class)).isEqualTo(idPago);
        assertThat(jdbc.queryForList("SELECT * FROM item_orden ORDER BY id"))
                .isEqualTo(itemsOriginales);
        Orden segunda = ordenes.findById(orden.getId()).orElseThrow();
        assertThat(segunda.getPagadoEn()).isEqualTo(primera.getPagadoEn());
        assertThat(segunda.getActualizadoEn()).isEqualTo(primera.getActualizadoEn());
    }

    @Test
    @DisplayName("24.5 - Un usuario autenticado no puede consultar el estado de una orden ajena")
    void consultaOrdenAjenaNoExponeEstado() throws Exception {
        // PREPARACIÓN: Ana es propietaria; Beto tiene un JWT válido, pero no es dueño.
        Orden orden = ordenPendiente();
        Usuario otroUsuario = guardarUsuario(TestDataFactory.beto());

        // CONTROL: el endpoint y el JWT funcionan para la propietaria.
        mockMvc.perform(get("/api/checkout/ordenes/{id}/estado", orden.getId())
                        .with(jwt.bearerToken(comprador)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string("\"PENDIENTE_PAGO\""));

        // EJECUCIÓN: misma URL, otro usuario real de la base y otro JWT.
        var respuesta = mockMvc.perform(get("/api/checkout/ordenes/{id}/estado", orden.getId())
                        .with(jwt.bearerToken(otroUsuario)))
                .andExpect(status().isNotFound()).andReturn();

        // VERIFICACIÓN: el 404 es la regla actual de findByIdAndUsuarioId;
        // no lo cambiamos a 403 ni permitimos filtrar información del recurso.
        assertThat(respuesta.getResponse().getContentAsString()).doesNotContain("PENDIENTE_PAGO");
        assertThat(ordenes.findById(orden.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoOrden.PENDIENTE_PAGO);
        assertThat(cantidad("pago")).isZero();
    }

    private Orden ordenPendiente() {
        Orden orden = new Orden();
        orden.setUsuario(comprador);
        orden.setEstado(EstadoOrden.PENDIENTE_PAGO);
        orden.setMoneda(TipoMoneda.ARG);
        orden.setSubtotal(new BigDecimal("200.00"));
        orden.setTotal(new BigDecimal("200.00"));
        orden.setCreadoEn(Instant.now());
        orden.setActualizadoEn(orden.getCreadoEn());
        orden.setVenceEn(orden.getCreadoEn().plus(Duration.ofHours(2)));
        ItemOrden item = new ItemOrden();
        item.setOrden(orden);
        item.setProductoId(UUID.randomUUID()); // Snapshot sin relación JPA al catálogo.
        item.setNombreProducto("Producto comprado");
        item.setPrecioUnitario(new BigDecimal("100.00"));
        item.setCantidad(2);
        orden.getItems().add(item);
        return ordenes.saveAndFlush(orden);
    }

    private Pago guardarPagoPendiente(Orden orden) {
        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setMonto(orden.getTotal());
        pago.setMoneda(orden.getMoneda());
        pago.setPreferenciaId("preferencia-test-001");
        pago.setUrlPago(URL_PAGO);
        pago.setCreadoEn(orden.getCreadoEn());
        pago.setActualizadoEn(orden.getCreadoEn());
        return pagos.saveAndFlush(pago);
    }

    private void esperarPagoAprobado(Orden orden, int consultas) {
        String respuesta = """
                {
                  "id": %s,
                  "order": {"id": 123456},
                  "status": "approved",
                  "external_reference": "%s",
                  "transaction_amount": 200.00,
                  "currency_id": "ARS",
                  "date_approved": "%s"
                }
                """.formatted(ID_PAGO, orden.getId(), FECHA_APROBACION);
        String ordenComercial = """
                {"id":123456,"preference_id":"preferencia-test-001","external_reference":"%s"}
                """.formatted(orden.getId());
        for (int i = 0; i < consultas; i++) {
            mercadoPago.expect(once(), requestTo(API + "/v1/payments/" + ID_PAGO))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("Authorization", "Bearer token-falso-exclusivo-checkout-test"))
                    .andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
            mercadoPago.expect(once(), requestTo(API + "/merchant_orders/123456"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("Authorization", "Bearer token-falso-exclusivo-checkout-test"))
                    .andRespond(withSuccess(ordenComercial, MediaType.APPLICATION_JSON));
        }
    }

    private void enviarWebhookFirmado(String requestId) throws Exception {
        String timestamp = TIMESTAMP_WEBHOOK;
        // Firma generada aquí, no con validarFirma() de producción:
        // si la validación real se rompe, el endpoint debe rechazar el aviso y fallará el test.
        String mensaje = "id:" + ID_PAGO + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO_WEBHOOK.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = HexFormat.of().formatHex(mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8)));
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .param("data.id", ID_PAGO)
                        .header("x-request-id", requestId)
                        .header("x-signature", "ts=" + timestamp + ",v1=" + firma)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment\",\"data\":{\"id\":\"" + ID_PAGO + "\"}}"))
                .andExpect(status().isOk());
    }

    private void verificarPagoAprobado(Orden original) {
        Orden guardada = ordenes.findById(original.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo(EstadoOrden.PAGADA);
        assertThat(guardada.getPaymentId()).isEqualTo(ID_PAGO);
        assertThat(guardada.getPagadoEn()).isEqualTo(FECHA_APROBACION);
        assertThat(cantidad("pago")).isEqualTo(1);
        Map<String, Object> pago = jdbc.queryForMap("SELECT * FROM pago WHERE pago_externo_id = ?", ID_PAGO);
        assertThat(pago.get("orden_id")).isEqualTo(original.getId());
        assertThat(pago.get("estado")).isEqualTo("APROBADO");
        assertThat((BigDecimal) pago.get("monto")).isEqualByComparingTo("200.00");
        assertThat(pago.get("moneda")).isEqualTo("ARG");
        assertThat(pago.get("preferencia_id")).isEqualTo("preferencia-test-001");
        assertThat(pago.get("url_pago")).isEqualTo(URL_PAGO);
    }

    private Usuario guardarUsuario(Usuario usuario) {
        usuario.setId(null); // Permite que JPA genere el UUID, igual que en los tests existentes.
        return usuarios.saveAndFlush(usuario);
    }

    private Carrito carritoActivo() {
        Carrito carrito = new Carrito();
        carrito.setUsuario(comprador);
        carrito.setEstadoCarrito(EstadoCarrito.ACTIVO);
        carrito.setCreadoEn(Instant.now());
        carrito.setActualizadoEn(Instant.now());
        carrito.setUltimaInteraccion(Instant.now());
        return carrito;
    }

    private void agregarItem(Carrito carrito, String nombre, String precioCatalogo,
                             String precioCopiado, int cantidad, Instant fechaPrecio) {
        Producto producto = productos.saveAndFlush(new Producto(
                nombre, "Producto exclusivo del test", new BigDecimal(precioCatalogo),
                "ARS", true, Instant.now()));
        ItemCarrito item = new ItemCarrito();
        item.setCarrito(carrito);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(new BigDecimal(precioCopiado));
        item.setPrecioAgregadoEn(fechaPrecio);
        item.setActualizadoEn(Instant.now());
        carrito.getItems().add(item);
    }

    private void verificarItemsCopiados(UUID ordenId, Carrito carrito) {
        var items = jdbc.queryForList("SELECT * FROM item_orden WHERE orden_id = ?", ordenId);
        assertThat(items).hasSize(3);
        Map<String, BigDecimal> preciosEsperados = Map.of(
                "Vigente", new BigDecimal("100"),
                "Vencido", new BigDecimal("200"),
                "Sin fecha", new BigDecimal("40"));
        Map<String, Integer> cantidadesEsperadas = Map.of("Vigente", 2, "Vencido", 3, "Sin fecha", 4);
        for (var fila : items) {
            String nombre = (String) fila.get("nombre_producto");
            assertThat(preciosEsperados).containsKey(nombre);
            assertThat((BigDecimal) fila.get("precio_unitario"))
                    .isEqualByComparingTo(preciosEsperados.get(nombre));
            assertThat(fila.get("cantidad")).isEqualTo(cantidadesEsperadas.get(nombre));
            ItemCarrito origen = carrito.getItems().stream()
                    .filter(i -> i.getProducto().getNombre().equals(nombre)).findFirst().orElseThrow();
            assertThat(fila.get("producto_id")).isEqualTo(origen.getProducto().getId());
            assertThat(fila.get("id")).isNotEqualTo(origen.getId());
        }
    }

    private long cantidad(String tabla) {
        // Los nombres vienen únicamente de constantes del test, nunca de una petición.
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracion {
        @Bean
        ClienteSimulado clienteSimulado() {
            RestClient.Builder builder = RestClient.builder();
            // Se enlaza antes de inyectar el builder en adapter y webhook.
            return new ClienteSimulado(builder, MockRestServiceServer.bindTo(builder).build());
        }

        @Bean
        @Primary
        RestClient.Builder restClientDePrueba(ClienteSimulado cliente) {
            return cliente.builder();
        }

        @Bean
        MockRestServiceServer servidorMercadoPago(ClienteSimulado cliente) {
            return cliente.servidor();
        }

        @Bean
        TaskScheduler taskScheduler() {
            // Evita que los cron reales alteren datos durante las verificaciones.
            return mock(TaskScheduler.class);
        }
    }

    record ClienteSimulado(RestClient.Builder builder, MockRestServiceServer servidor) { }
}
