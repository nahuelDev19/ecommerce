package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.config.LimitesImportacion;
import com.nahuel.ecommerce.dtos.ImportacionJobDto;
import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.ImportacionJob;
import com.nahuel.ecommerce.exceptions.*;
import com.nahuel.ecommerce.pipeline.ImportacionAsyncService;
import com.nahuel.ecommerce.repositories.ImportacionJobRepository;
import com.nahuel.ecommerce.security.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportacionServiceImp implements ImportacionService {

    private final ImportacionJobRepository importacionJobRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ImportacionAsyncService importacionAsyncService;
    private final LimitesImportacion limitesImportacion;

    @Override
    public UUID subirArchivo(MultipartFile archivo,boolean soloValidacion) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoVacioException();
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || (!nombre.endsWith(".xls") && !nombre.endsWith(".xlsx"))) {
            throw new FormatoInvalidoException(nombre);
        }

        Path destino = crearDestino();

        // Copia a disco y calcula la huella en una sola pasada.
        String huella = copiarYCalcularHuella(archivo, destino);

        if(!soloValidacion){
            Optional<ImportacionJob> existente = importacionJobRepository.findFirstByHuellaContenidoAndSoloValidacionFalseOrderByFechaCreacionDesc(huella);
            if (existente.isPresent() && existente.get().getEstado()!= EstadoImportacion.FALLIDOS) {
                borrarArchivo(destino);
                throw new ArchivoDuplicadoException(existente.get().getId());
            }

        }

        ImportacionJob job = new ImportacionJob();
        job.setUsuario(usuarioAutenticadoService.actual());
        job.setNombreArchivo(nombre);
        job.setRutaArchivoTemporal(destino.toString());
        job.setHuellaContenido(huella);
        job.setEstado(EstadoImportacion.PENDIENTE);
        job.setSoloValidacion(soloValidacion);
        ImportacionJob guardado = importacionJobRepository.save(job);

        // El disparo va después del commit: si corriera acá, el hilo del pool podría
        // buscar el job antes de que la fila exista y no lo encontraría.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                importacionAsyncService.importarEnSegundoPlano(destino, guardado.getId());
            }
        });
        return guardado.getId();
    }

    private Path crearDestino() {
        try {
            Path carpeta = Path.of(limitesImportacion.getDirectorioTemporal());
            Files.createDirectories(carpeta);
            return carpeta.resolve(UUID.randomUUID() + ".xlsx");
        } catch (IOException e) {
            throw new ErrorProcesamientoArchivoException("No se pudo crear el archivo temporal", e);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ImportacionJobDto consulta(UUID id){
        ImportacionJob job = importacionJobRepository.findById(id).orElseThrow(()-> new RecursoNoEncontradoException("Importacion no encontrado"));
        UUID idUsuarioActual= usuarioAutenticadoService.actual().getId();
        if(!job.getUsuario().getId().equals(idUsuarioActual)){
            throw new RecursoNoEncontradoException("Importacion no encontrado");
        }

        return ImportacionJobDto.builder()
                .id(job.getId())
                .nombreArchivo(job.getNombreArchivo())
                .estado(job.getEstado())
                .soloValidacion(job.isSoloValidacion())
                .cantidadFilas(job.getCantidadFilas())
                .productosImportados(job.getProductosImportados())
                .filasConError(job.getFilasConError())
                .detalleErrores(job.getDetalleErrores())
                .mensajeError(job.getMensajeError())
                .fechaCreacion(job.getFechaCreacion())
                .fechaFinalizacion(job.getFechaFinalizacion())
                .build();
    }

    private String copiarYCalcularHuella(MultipartFile archivo, Path destino) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(archivo.getInputStream(), md)) {
                Files.copy(in, destino);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ErrorProcesamientoArchivoException("No se pudo procesar el archivo", e);
        }
    }

    private void borrarArchivo(Path destino) {
        try {
            Files.deleteIfExists(destino);
        } catch (IOException e) {
            throw new ErrorProcesamientoArchivoException("No se pudo eliminar el archivo temporal", e);
        }
    }

    /*
    La huella se calcula sobre el contenido porque lo que queremos detectar es si dos archivos son el mismo archivo, más allá de cómo se llamen.
    El nombre es un dato arbitrario que el usuario puede cambiar libremente y no dice nada sobre los bytes del archivo.

    Situación 1 (falso negativo con el nombre): subo ventas_enero.xlsx y luego copia_ventas_enero.xlsx.
    Ambos tienen exactamente el mismo contenido. Si hasheara por nombre, el sistema los vería distintos y permitiría subir el mismo archivo dos veces.
    Con hash por contenido, el segundo se detecta como duplicado.

    Situación 2 (falso positivo con el nombre): subo productos.xlsx con 100 filas hoy, y mañana subo otro productos.xlsx con 150 filas (datos nuevos).
    Mismo nombre, contenido distinto. Si hasheara por nombre, el sistema rechazaría el segundo como "duplicado" y perdería la carga nueva.
    Con hash por contenido, detecta que son distintos y lo acepta.
     */
}
