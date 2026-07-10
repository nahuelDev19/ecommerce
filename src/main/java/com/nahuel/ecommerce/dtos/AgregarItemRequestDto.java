package com.nahuel.ecommerce.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class AgregarItemRequestDto {

    private UUID productoId;
    private Integer cantidad;


}
