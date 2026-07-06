package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;

import java.math.BigDecimal;
import java.time.Instant;

public class CrearCuponRequestDto {

    private String codigo;
    private Boolean activo;
    private AlcanceCupon alcanceCupon;
    private TipoDescuento tipoDescuento;
    private BigDecimal valorDescuento;
    private BigDecimal montoMinimoCarrito;
    private Instant iniciaEn;
    private Instant terminaEn;
    //si aplica por producto:
    //productoIds: List<UUID> (esto es clave para “habilitados por producto”)


}
