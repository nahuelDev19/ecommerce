package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CuponDto {

    private UUID id;
    private String codigoCupon;
    private Boolean activo;
    private AlcanceCupon alcanceCupon;
    private TipoDescuento tipoDescuento;
    private BigDecimal valorDescuento;
    private String moneda;
    private BigDecimal montoMinimoCarrito;
    private Integer limiteUsoTotal;
    private Integer limiteUsoPorUsuario;
    private Instant iniciadoEn;
    private Instant terminaEn;


}
