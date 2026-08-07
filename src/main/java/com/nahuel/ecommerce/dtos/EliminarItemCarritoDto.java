package com.nahuel.ecommerce.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EliminarItemCarritoDto {

    @NotNull(message = "El producto es obligatorio")
    private UUID productoId;
}
