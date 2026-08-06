package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class UsuarioInactivoException extends AppException{

    public UsuarioInactivoException() {
        super(HttpStatus.FORBIDDEN, "USER_INACTIVE", "El usuario no está disponible en este momento");
    }
}
