package com.nahuel.ecommerce.integracion;

import com.jayway.jsonpath.JsonPath;
import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.ImportacionJob;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.ImportacionJobRepository;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.support.ExcelTestFactory;
import com.nahuel.ecommerce.support.JwtTestHelper;
import com.nahuel.ecommerce.support.TestDataFactory;
import com.nahuel.ecommerce.support.TestDeIntegracion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@AutoConfigureMockMvc
class ImportacionIntegracionTest extends TestDeIntegracion {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ImportacionJobRepository importacionJobRepository;

    private final JwtTestHelper jwt = new JwtTestHelper();
    private Usuario admin;

    @BeforeEach
    void prepararBase() {
        // Orden por FK: importacion_job.usuario_id referencia a usuario.
        importacionJobRepository.deleteAll();
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario nuevoAdmin = TestDataFactory.admin();
        nuevoAdmin.setId(null);
        admin = usuarioRepository.save(nuevoAdmin);
    }

    @Test
    @DisplayName("19 - Excel válido con 3 productos: COMPLETADO y los 3 en la base con su precio")
    void excelValidoConTresProductos() throws Exception {
        // ---- BLOQUE 1: armo el dato ----
        byte[] excel = ExcelTestFactory.excel(new Object[][]{
                // nombre,    descripcion,            moneda, precio,        activo
                {"Teclado", "Teclado mecánico",  "USD",  49,           true},
                {"Mouse",   "Mouse inalámbrico", "ARS",  15000,        true},
                {"Monitor", "Monitor 24 pulgadas","ARS", "250000,50",  false}
        });

        // ---- BLOQUE 2: ejecuto (subo el archivo) ----
        MvcResult respuesta = subirExcel(excel, "productos.xlsx", admin);
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(202);

        String cuerpo = respuesta.getResponse().getContentAsString();
        UUID idJob = UUID.fromString(JsonPath.read(cuerpo, "$"));

        // ---- BLOQUE 3: espero a que el trabajo termine ----
        ImportacionJob job = esperarTrabajo(idJob);

        // ---- BLOQUE 4: verifico contra la base ----
        assertThat(job.getEstado()).isEqualTo(EstadoImportacion.COMPLETADO);
        assertThat(job.getCantidadFilas()).isEqualTo(3);
        assertThat(job.getProductosImportados()).isEqualTo(3);
        assertThat(job.getFechaFinalizacion()).isNotNull();

        Map<String, BigDecimal> precios = productoRepository.findAll().stream()
                .collect(toMap(Producto::getNombre, Producto::getPrecioBase));

        assertThat(precios).containsOnlyKeys("Teclado", "Mouse", "Monitor");
        assertThat(precios.get("Teclado")).isEqualByComparingTo(new BigDecimal("49"));
        assertThat(precios.get("Mouse")).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(precios.get("Monitor")).isEqualByComparingTo(new BigDecimal("250000.50"));
    }

    @Test
    @DisplayName("20 - Si una fila está mal, no se guarda ninguna")
    void noGuardaNadaSiUnaFilaEstaMal() throws Exception {
        byte[] excel = ExcelTestFactory.excel(new Object[][]{
                // ---- 3 filas buenas ----
                {"Teclado", "Teclado mecánico", "USD", 49, true},
                {"Mouse", "Mouse inalámbrico", "ARS", 15000, true},
                {"Monitor", "Monitor 24 pulgadas", "ARS", "250000,50", false},
                // ---- 2 filas malas: rompen la validación de formato del pipeline ----
                {"Auriculares", "Precio no numérico", "ARS", "abc", true},
                {"Parlantes", "Moneda inexistente", "XXX", 100, true}
        });

        MvcResult respuesta = subirExcel(excel, "productos.xlsx", admin);

        // El upload contesta 202 IGUAL: el rechazo ocurre después, en el hilo del pool.
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(202);
        UUID idJob = UUID.fromString(JsonPath.read(respuesta.getResponse().getContentAsString(), "$"));

        ImportacionJob job = esperarTrabajo(idJob);
        // al comentar el if en ProcesarArchivoService confirmar() genera un fallo en el test
        // porque los productos pasarin a estar guardados
        assertThat(job.getEstado()).isEqualTo(EstadoImportacion.COMPLETA_CON_ERRORES);
        assertThat(job.getFechaFinalizacion()).isNotNull();
        assertThat(job.getProductosImportados()).isZero();

        // Lo importante del caso: ni siquiera las 3 filas BUENAS se guardaron.
        assertThat(productoRepository.existsByNombre("Teclado")).isFalse();
        assertThat(productoRepository.existsByNombre("Mouse")).isFalse();
        assertThat(productoRepository.existsByNombre("Monitor")).isFalse();
        assertThat(productoRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("22 - Un producto que ya existe no se duplica ni se pisa su precio")
    void noDuplicaNiPisaUnProductoExistente() throws Exception {
        // ---- DADO: ya existe "Teclado" a 100 en la base ----
        productoRepository.save(new Producto(
                "Teclado", "El que ya estaba", new BigDecimal("100"), "ARS", true, Instant.now()));

        // ---- CUANDO: subo un Excel que trae ese mismo nombre (a OTRO precio) y 2 nuevos ----
        byte[] excel = ExcelTestFactory.excel(new Object[][]{
                // nombre,    descripcion,           moneda, precio,       activo
                {"Teclado", "Intento de pisarlo", "ARS", 999, true},
                {"Mouse", "Mouse inalámbrico", "ARS", 15000, true},
                {"Monitor", "Monitor 24 pulgadas", "ARS", "250000,50", false}
        });

        MvcResult respuesta = subirExcel(excel, "productos.xlsx", admin);
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(202);
        UUID idJob = UUID.fromString(JsonPath.read(respuesta.getResponse().getContentAsString(), "$"));

        // ---- ENTONCES: el trabajo guardó sólo las 2 nuevas ----
        ImportacionJob job = esperarTrabajo(idJob);
        assertThat(job.getEstado()).isEqualTo(EstadoImportacion.COMPLETADO);
        assertThat(job.getCantidadFilas()).isEqualTo(3);        // leyó las 3 filas del Excel
        assertThat(job.getProductosImportados()).isEqualTo(2);  // pero guardó sólo 2

        // ---- Y en la base quedan 3: el que ya estaba + los 2 nuevos ----
        Map<String, BigDecimal> precios = productoRepository.findAll().stream()
                .collect(toMap(Producto::getNombre, Producto::getPrecioBase));

        assertThat(precios).containsOnlyKeys("Teclado", "Mouse", "Monitor");
        // Lo central: el repetido conserva SU precio (100), no el del Excel (999).
        assertThat(precios.get("Teclado")).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(precios.get("Mouse")).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(precios.get("Monitor")).isEqualByComparingTo(new BigDecimal("250000.50"));
    }

    @Test
    @DisplayName("23 - El mismo archivo dos veces: la segunda se rechaza con 409 y el id de la primera")
    void rechazaElMismoArchivoSubidoDosVeces() throws Exception {
        byte[] excel = ExcelTestFactory.excel(new Object[][]{
                // nombre,    descripcion,            moneda, precio,        activo
                {"Teclado", "Teclado mecánico", "USD", 49, true},
                {"Mouse", "Mouse inalámbrico", "ARS", 15000, true},
                {"Monitor", "Monitor 24 pulgadas", "ARS", "250000,50", false}
        });

        // ---- Primera subida: se acepta y termina bien ----
        MvcResult primera = subirExcel(excel, "productos.xlsx", admin);
        assertThat(primera.getResponse().getStatus()).isEqualTo(202);
        UUID idPrimera = UUID.fromString(JsonPath.read(primera.getResponse().getContentAsString(), "$"));

        ImportacionJob job = esperarTrabajo(idPrimera);
        assertThat(job.getEstado()).isEqualTo(EstadoImportacion.COMPLETADO);
        assertThat(productoRepository.count()).isEqualTo(3);

        // ---- Segunda subida: MISMO contenido (mismo hash) -> rechazada antes de crear el trabajo ----
        MvcResult segunda = subirExcel(excel, "productos.xlsx", admin);

        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);
        String cuerpo = segunda.getResponse().getContentAsString();
        // JsonPath.read es genérico: sin tipo destino, assertThat(...) queda ambiguo.
        // Asignando a una variable String, el tipo queda fijo y compila.
        String titulo = JsonPath.read(cuerpo, "$.title");
        String importacionId = JsonPath.read(cuerpo, "$.importacionId");
        assertThat(titulo).isEqualTo("ARCHIVO_DUPLICADO");
        assertThat(importacionId).isEqualTo(idPrimera.toString());

        // ---- Y no se duplicó nada: un solo trabajo, una sola tanda de productos ----
        assertThat(importacionJobRepository.count()).isEqualTo(1);
        assertThat(productoRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("26 - Sin token: 401 y la base queda intacta")
    void sinTokenNoSeImportaNada() throws Exception {
        byte[] excel = ExcelTestFactory.excel(new Object[][]{
                // nombre,    descripcion,            moneda, precio,        activo
                {"Teclado", "Teclado mecánico", "USD", 49, true},
                {"Mouse", "Mouse inalámbrico", "ARS", 15000, true},
                {"Monitor", "Monitor 24 pulgadas", "ARS", "250000,50", false}
        });

        MvcResult respuesta = subirExcelSinToken(excel, "productos.xlsx");

        // El 401 sale del authenticationEntryPoint de SecurityConfig, NO del GlobalExceptionHandler:
        // por eso no hay ProblemDetail en el body, y sólo se assertea el status.
        assertThat(respuesta.getResponse().getStatus()).isEqualTo(401);

        // Y acá no hay nada que esperar: sin request autenticada nunca se creó un trabajo.
        assertThat(importacionJobRepository.count()).isZero();
        assertThat(productoRepository.count()).isZero();
    }

    // ---------- helpers reusables ----------

    private MvcResult subirExcel(byte[] contenido, String nombreArchivo, Usuario quien) throws Exception {
        return mockMvc.perform(multipart("/api/productos/upload")
                        .file(new MockMultipartFile("archivo", nombreArchivo, XLSX, contenido))
                        .with(jwt.bearerToken(quien)))
                .andReturn();
    }

    /** Igual que subirExcel, pero SIN header Authorization: es el caso del 401. */
    private MvcResult subirExcelSinToken(byte[] contenido, String nombreArchivo) throws Exception {
        return mockMvc.perform(multipart("/api/productos/upload")
                        .file(new MockMultipartFile("archivo", nombreArchivo, XLSX, contenido)))
                .andReturn();
    }

    private ImportacionJob esperarTrabajo(UUID idJob) {
        await().alias("importación " + idJob + " terminada")
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(importacionJobRepository.findById(idJob)
                        .orElseThrow().getEstado())
                        .isNotEqualTo(EstadoImportacion.PENDIENTE));
        return importacionJobRepository.findById(idJob).orElseThrow();
    }
}