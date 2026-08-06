package com.nahuel.ecommerce.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ItemRequestDto {

    @NotNull(message = "El usuario es obligatorio")
    private UUID usuarioId;

    @NotNull(message = "El producto es obligatorio")
    private UUID productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;


}
