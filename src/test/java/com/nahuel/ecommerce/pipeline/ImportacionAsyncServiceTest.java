package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ErrorImportacionDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.ImportacionJob;
import com.nahuel.ecommerce.exceptions.ArchivoConErroresException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.ImportacionJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Prueba el servicio real con dependencias simuladas y archivos temporales reales.
 * La llamada es directa: no prueba el proxy de @Async, hilos ni persistencia JPA.
 */
@ExtendWith(MockitoExtension.class)
class ImportacionAsyncServiceTest {

    @Mock private ProcesarArchivosService procesador;
    @Mock private ImportacionJobRepository jobs;
    @TempDir Path directorioTemporal;

    private ImportacionAsyncService servicio;
    private ImportacionJob job;
    private Path archivo;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new ImportacionAsyncService(procesador, jobs);
        job = new ImportacionJob();
        job.setId(UUID.randomUUID());
        job.setEstado(EstadoImportacion.PENDIENTE);
        archivo = Files.writeString(directorioTemporal.resolve("productos.xlsx"), "archivo del test");
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
    }

    @ParameterizedTest(name = "Procesamiento exitoso, soloValidacion={0}")
    @ValueSource(booleans = {false, true})
    void resultadoExitoso_guardaContadoresYFinaliza(boolean soloValidacion) {
        job.setSoloValidacion(soloValidacion);
        ResultadoImportacionDto resultado = new ResultadoImportacionDto();
        resultado.setLineasProcesadas(5);
        resultado.setProductosImportados(soloValidacion ? 0 : 3);
        when(procesador.procesarArchivo(archivo, soloValidacion)).thenReturn(resultado);
        LocalDateTime inicio = LocalDateTime.now();

        servicio.importarEnSegundoPlano(archivo, job.getId());

        ImportacionJob guardado = verificarFinalizacion(inicio);
        assertThat(guardado.getEstado()).isEqualTo(EstadoImportacion.COMPLETADO);
        assertThat(guardado.getCantidadFilas()).isEqualTo(5);
        assertThat(guardado.getProductosImportados()).isEqualTo(soloValidacion ? 0 : 3);
        assertThat(guardado.getMensajeError()).isNull();
        verify(procesador).procesarArchivo(archivo, soloValidacion);
    }

    @Test
    void archivoConErrores_conservaReporteYNoLoTrataComoFalloGeneral() {
        ResultadoImportacionDto reporte = new ResultadoImportacionDto();
        reporte.setLineasProcesadas(4);
        reporte.setErrores(List.of(
                new ErrorImportacionDto("Mouse", "Precio inválido", 2),
                new ErrorImportacionDto(null, "Falta nombre", 4)));
        when(procesador.procesarArchivo(archivo, false))
                .thenThrow(new ArchivoConErroresException("Archivo con errores", reporte));
        LocalDateTime inicio = LocalDateTime.now();

        servicio.importarEnSegundoPlano(archivo, job.getId());

        ImportacionJob guardado = verificarFinalizacion(inicio);
        assertThat(guardado.getEstado()).isEqualTo(EstadoImportacion.COMPLETA_CON_ERRORES);
        assertThat(guardado.getCantidadFilas()).isEqualTo(4);
        assertThat(guardado.getFilasConError()).isEqualTo(2);
        assertThat(guardado.getDetalleErrores()).isEqualTo(
                "Fila 2 (Mouse): Precio inválido\nFila 4: Falta nombre");
        assertThat(guardado.getMensajeError()).isNull();
    }

    // Cada mensaje se prueba tanto con Exception como con Error.
    // AssertionError es controlado: no provocamos falta de memoria ni daños a la JVM.
    static Stream<Arguments> fallos() {
        return Stream.of(false, true).flatMap(esError -> Stream.of(
                Arguments.of(esError, null, null),
                Arguments.of(esError, "Fallo breve", "Fallo breve"),
                Arguments.of(esError, "a".repeat(255), "a".repeat(255)),
                Arguments.of(esError, "a".repeat(255) + "b".repeat(45), "a".repeat(255))));
    }

    @ParameterizedTest(name = "Fallo {index}: Error={0}")
    @MethodSource("fallos")
    void falloGeneral_guardaEstadoYMensajeAcotado(boolean esError, String mensaje, String esperado) {
        Throwable fallo = esError ? new AssertionError((Object) mensaje) : new RuntimeException(mensaje);
        // AssertionError(Object null) produce el texto "null"; usamos el constructor
        // sin argumentos para probar un mensaje realmente nulo.
        if (esError && mensaje == null) {
            fallo = new AssertionError();
        }
        when(procesador.procesarArchivo(archivo, false)).thenThrow(fallo);
        LocalDateTime inicio = LocalDateTime.now();

        assertThatCode(() -> servicio.importarEnSegundoPlano(archivo, job.getId()))
                .doesNotThrowAnyException();

        ImportacionJob guardado = verificarFinalizacion(inicio);
        assertThat(guardado.getEstado()).isEqualTo(EstadoImportacion.FALLIDOS);
        // Comprueba el contenido exacto, no solo la longitud del mensaje.
        assertThat(guardado.getMensajeError()).isEqualTo(esperado);
    }

    @Test
    void jobInexistente_rechazaSinProcesarNiGuardar() {
        when(jobs.findById(job.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.importarEnSegundoPlano(archivo, job.getId()))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(procesador);
        verify(jobs, never()).save(any());
        // La búsqueda sucede antes del try/finally: este caso no elimina el archivo.
        // Documentamos el comportamiento actual sin cambiar producción.
        assertThat(archivo).exists();
    }

    private ImportacionJob verificarFinalizacion(LocalDateTime inicio) {
        ArgumentCaptor<ImportacionJob> captor = ArgumentCaptor.forClass(ImportacionJob.class);
        verify(jobs).save(captor.capture());
        ImportacionJob guardado = captor.getValue();
        assertThat(guardado.getId()).isEqualTo(job.getId());
        assertThat(guardado.getFechaFinalizacion()).isNotNull()
                .isBetween(inicio, LocalDateTime.now());
        assertThat(archivo).doesNotExist();
        return guardado;
    }
}
