package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.EstadoCarrito;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
@Data
public class CarritoDto {

    private UUID id;
    private EstadoCarrito estadoCarrito;
    @NotNull(message = "El usuario es obligatorio")
    private UUID usuarioId;
    private List<ItemCarritoDto> items;
    private BigDecimal descuentoTotal;
    private BigDecimal subtotal;
    private Instant creadoEn;
    private Instant actualizadoEn;
    private Instant ultimaInteraccion;
    private BigDecimal total;
    /*

    private List<ItemCarritoDto> items;
    //private TotalesCarritoDto totalesCarritoDto;
    private CuponDto cuponAplicado;


     */

}
