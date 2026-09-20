package com.nahuel.ecommerce.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ErrorImportacionDto {


    private String  nombre;
    private String error;
    private int numeroFila;

}
