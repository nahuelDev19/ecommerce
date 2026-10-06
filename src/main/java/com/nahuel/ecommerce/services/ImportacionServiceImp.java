package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.config.LimitesImportacion;
import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.ImportacionJob;
import com.nahuel.ecommerce.exceptions.ArchivoDuplicadoException;
import com.nahuel.ecommerce.exceptions.ArchivoVacioException;
import com.nahuel.ecommerce.exceptions.ErrorProcesamientoArchivoException;
import com.nahuel.ecommerce.exceptions.FormatoInvalidoException;
import com.nahuel.ecommerce.pipeline.ImportacionAsyncService;
import com.nahuel.ecommerce.repositories.ImportacionJobRepository;
import com.nahuel.ecommerce.security.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public UUID subirArchivo(MultipartFile archivo) {
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

        Optional<ImportacionJob> existente = importacionJobRepository.findByhuellaContenido(huella);
        if (existente.isPresent()) {
            borrarArchivo(destino);
            throw new ArchivoDuplicadoException(existente.get().getId());
        }

        ImportacionJob job = new ImportacionJob();
        job.setUsuario(usuarioAutenticadoService.actual());
        job.setNombreArchivo(nombre);
        job.setRutaArchivoTemporal(destino.toString());
        job.setHuellaContenido(huella);
        job.setEstado(EstadoImportacion.PENDIENTE);

        ImportacionJob guardado = importacionJobRepository.save(job);

        importacionAsyncService.importarEnSegundoPlano(destino);
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
}
