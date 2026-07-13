package com.nahuel.ecommerce.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class EliminarItemCarritoDto {

    private UUID usuarioId;
    private UUID productoId;

}
