package com.nahuel.ecommerce.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Getter
public class ArchivoDuplicadoException extends AppException {

    private final UUID importacionAnterior;

    public ArchivoDuplicadoException(UUID importacionAnterior) {
        super(HttpStatus.CONFLICT, "ARCHIVO_DUPLICADO", "El archivo ya fue importado previamente");
        this.importacionAnterior = importacionAnterior;
    }
}
