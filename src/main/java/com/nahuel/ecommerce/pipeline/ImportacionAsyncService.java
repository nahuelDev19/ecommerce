package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.ImportacionJob;
import com.nahuel.ecommerce.exceptions.ArchivoConErroresException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.ImportacionJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

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
        ResultadoImportacionDto r= procesarArchivosService.procesarArchivo(file);
        job.setEstado(EstadoImportacion.COMPLETADO);
        job.setProductosImportados(r.getProductosImportados());
        job.setCantidadFilas(r.getLineasProcesadas());

        }catch (ArchivoConErroresException e){
            //termina pero con errores
            job.setEstado(EstadoImportacion.COMPLETA_CON_ERRORES);
        }catch (Exception e){
            //aqui se rompe
            job.setEstado(EstadoImportacion.FALLIDOS);
            job.setMensajeError(e.getMessage());
        }
            finally {
        job.setFechaFinalizacion(LocalDateTime.now());
        importacionJobRepository.save(job);
        }

    }
}