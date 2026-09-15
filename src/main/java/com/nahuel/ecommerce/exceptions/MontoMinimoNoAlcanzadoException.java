package com.nahuel.ecommerce.exceptions;

import org.springframework.http.HttpStatus;

public class MontoMinimoNoAlcanzadoException extends ReglaNegocioException{


    public MontoMinimoNoAlcanzadoException(String message) {
        super(message);
    }
}
