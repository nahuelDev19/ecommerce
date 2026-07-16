package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.EstadoCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;
import com.nahuel.ecommerce.entitys.TipoMoneda;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Getter @Setter
public class CuponDtoResponse {

    private UUID id;
    private String codigoCupon;
    private EstadoCupon estadoCupon=EstadoCupon.ACTIVO;
    private AlcanceCupon alcanceCupon;
    private TipoDescuento tipoDescuento;
    private BigDecimal valorDescuento;
    private TipoMoneda moneda;
    private BigDecimal montoMinimoCarrito;
    private Integer limiteUsoPorUsuario=1;
    private String descripcion;
    private Instant iniciadoEn;
    private Instant terminaEn;

}
