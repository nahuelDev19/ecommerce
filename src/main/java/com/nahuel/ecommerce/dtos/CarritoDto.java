package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.EstadoCarrito;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@Data
public class CarritoDto {

    private UUID id;
    private EstadoCarrito estadoCarrito;
    private UUID usuarioId;
    private List<ItemCarritoDto> items;
    private BigDecimal descuentoTotal;
    private BigDecimal subtotal;
    private BigDecimal total;
    /*

    private List<ItemCarritoDto> items;
    //private TotalesCarritoDto totalesCarritoDto;
    private CuponDto cuponAplicado;


     */

}
