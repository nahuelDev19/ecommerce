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

    @NotBlank(message = "")
    @Size(max=150, message = "")
    private String nombre;

    @NotBlank
    @Size(max=2000, message = "")
    private String descripcion;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioBase;

    @NotBlank
    @Size(min = 3, max = 3, message = "")
    private String moneda;

    private Boolean activo;

    private Instant fechaDescontinuado;

}
