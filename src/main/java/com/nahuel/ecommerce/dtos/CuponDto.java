package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CuponDto {

    private UUID id;
    private String codigo;
    private boolean activo;
    private AlcanceCupon alcanceCupon;
    private TipoDescuento tipoDescuento;
    private BigDecimal valorDescuento;
    private BigDecimal montoMinimoCarrito;
    //private Instant iniciadoEn;
    //private Instant terminaEn;


}
