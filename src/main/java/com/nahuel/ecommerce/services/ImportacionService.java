package com.nahuel.ecommerce.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImportacionService {

    UUID subirArchivo(MultipartFile archivo);
}
