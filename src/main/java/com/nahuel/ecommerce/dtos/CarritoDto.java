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
    /*private UUID id;
    private EstadoCarrito estadoCarrito;
    private List<ItemCarritoDto> items;
    //private TotalesCarritoDto totalesCarritoDto;
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal total;
    private CuponDto cuponAplicado;


     */

}
