package com.nahuel.ecommerce.pipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class ImportacionAsyncService {

    private final ProcesarArchivosService procesarArchivosService;

    // @Async("importacionExecutor") = "este método corre en el pool importacionExecutor,
    // NO en el hilo de Tomcat que atiende el request".
    @Async("importacionExecutor")
    public void importarEnSegundoPlano(Path file) {
        // Todo lo que está acá adentro corre en un hilo del pool (importacion-1, importacion-2...)
        procesarArchivosService.procesarArchivo(file);
    }
}