package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ErrorImportacionDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.ImportacionJob;
import com.nahuel.ecommerce.exceptions.ArchivoConErroresException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.ImportacionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportacionAsyncService {

    private final ProcesarArchivosService procesarArchivosService;

    private final ImportacionJobRepository importacionJobRepository;
    // @Async("importacionExecutor") = "este método corre en el pool importacionExecutor,
    // NO en el hilo de Tomcat que atiende el request".
    @Async("importacionExecutor")
    public void importarEnSegundoPlano(Path file, UUID idJob) {
        ImportacionJob job = importacionJobRepository.findById(idJob)
                .orElseThrow(() -> new RecursoNoEncontradoException("El Job no fue encontrado"));
        try {
        // Todo lo que está acá adentro corre en un hilo del pool (importacion-1, importacion-2...)
        ResultadoImportacionDto r= procesarArchivosService.procesarArchivo(file, job.isSoloValidacion());
        job.setEstado(EstadoImportacion.COMPLETADO);
        job.setProductosImportados(r.getProductosImportados());
        job.setCantidadFilas(r.getLineasProcesadas());

        }catch (ArchivoConErroresException e){
            // El reporte viaja adentro de la excepción: si no lo leo acá, se pierde.
            ResultadoImportacionDto reporte = e.getReporte();

            job.setEstado(EstadoImportacion.COMPLETA_CON_ERRORES);
            job.setCantidadFilas(reporte.getLineasProcesadas());
            job.setFilasConError(cantidadDeFilasConError(reporte));
            job.setDetalleErrores(serializarErrores(reporte.getErrores()));
        }catch (Exception e){
            //aqui se rompe
            job.setEstado(EstadoImportacion.FALLIDOS);
            job.setMensajeError(e.getMessage());
        }
            finally {
        job.setFechaFinalizacion(LocalDateTime.now());
        importacionJobRepository.save(job);

        borrarArchivoTemporal(file);
        }

    }

    private void borrarArchivoTemporal(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {

            log.warn("No se pudo borrar el archivo temporal {}", archivo, e);
        }
    }

    // Cada entrada de la lista de errores ES una fila descartada (TransformacionDatosService
    // agrega una por fila), así que la cantidad de filas con error es el tamaño de la lista.
    private int cantidadDeFilasConError(ResultadoImportacionDto reporte) {
        if (reporte.getErrores() == null) {
            return 0;
        }
        return reporte.getErrores().size();
    }

    // El campo del job es String (detalle_errores es TEXT en la base), así que acá se
    // arma el texto: una línea por fila con error.
    private String serializarErrores(List<ErrorImportacionDto> errores) {
        if (errores == null || errores.isEmpty()) {
            return null;
        }
        return errores.stream()
                .map(error -> "Fila " + error.getNumeroFila()
                        + (error.getNombre() == null ? "" : " (" + error.getNombre() + ")")
                        + ": " + error.getError())
                .collect(Collectors.joining("\n"));
    }
}
