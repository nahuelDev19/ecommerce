package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;
import com.nahuel.ecommerce.entitys.TipoMoneda;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class CuponDto {

    @NotBlank(message = "El código del cupón es obligatorio")
    @Size(min = 3, max = 30, message = "El código debe tener entre 3 y 30 caracteres")
    private String codigoCupon;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    @NotNull(message = "El alcance del cupón es obligatorio")
    private AlcanceCupon alcanceCupon;

    @NotNull(message = "El tipo de descuento es obligatorio")
    private TipoDescuento tipoDescuento;

    @NotNull(message = "El valor del descuento es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true,
            message = "El valor del descuento debe ser mayor a 0")
    private BigDecimal valorDescuento;

    @NotNull(message = "La moneda es obligatoria")
    private TipoMoneda moneda;

    @NotNull(message = "El monto mínimo del carrito es obligatorio")
    @DecimalMin(value = "0.00", inclusive = true,
            message = "El monto mínimo no puede ser negativo")
    private BigDecimal montoMinimoCarrito;

    @NotNull(message = "El límite de uso por usuario es obligatorio")
    @Min(value = 1, message = "El límite de uso debe ser al menos 1")
    private Integer limiteUsoPorUsuario;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private Instant iniciadoEn;

    @NotNull(message = "La fecha de finalización es obligatoria")
    private Instant terminadoEn;

}
