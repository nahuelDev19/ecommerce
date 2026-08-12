package com.nahuel.ecommerce.exceptions;


import org.springframework.http.HttpStatus;

/** ARCHIVO NUEVO. 401: no sabemos quien sos. Distinto de AccesoDenegadoException (403). */
public class NoAutenticadoException extends AppException {
    public NoAutenticadoException() {
        super(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "Se requiere autenticacion");
    }
}