package com.nahuel.ecommerce.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ErrorImportacionDto {


    private String  fila;
    private String error;
}
