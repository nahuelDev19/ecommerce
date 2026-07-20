package com.nahuel.ecommerce.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDto {

    private UUID id;

    @NotBlank(message = "Nombre no puede ser vacio o tener espacios en blanco ")
    @Size(max=150, message = "Nombre no puede superar los 150 caracteres")
    private String nombre;

    @NotBlank(message = "Descripcion no puede ser vacio o tener espacios en blanco ")
    @Size(max=2000, message = "Descripcion no puede superar los 2000 caracteres")
    private String descripcion;

    @NotNull(message = "Precio no puede ser null")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioBase;

    @NotBlank(message = "Moneda no puede ser vacio o tener espacios en blanco ")
    @Size(min = 3, max = 3, message = "")
    private String moneda;

    @NotNull(message = "Estado del carrito no puede ser nullo")
    private Boolean activo;

    private Instant fechaDescontinuado;

}
