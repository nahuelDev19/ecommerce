package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class ProductoYaRegistradoException extends AppException{
    public ProductoYaRegistradoException(String producto) {
        super(HttpStatus.CONFLICT, "PRODUCT_ALREADY_REGISTERED", "El producto "+producto+" ya se encuentra registrado");
    }
}
