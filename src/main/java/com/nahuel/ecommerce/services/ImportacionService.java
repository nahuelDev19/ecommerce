package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.ImportacionJobDto;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImportacionService {

    UUID subirArchivo(MultipartFile archivo, boolean soloValidacion);

    @Transactional(readOnly = true)
    ImportacionJobDto consulta(UUID id);
}
