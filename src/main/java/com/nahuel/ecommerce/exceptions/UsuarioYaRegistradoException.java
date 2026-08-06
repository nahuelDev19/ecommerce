package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class UsuarioYaRegistradoException extends AppException{
    public UsuarioYaRegistradoException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "El email ingresado ya se encuentra registrado");
    }
}
