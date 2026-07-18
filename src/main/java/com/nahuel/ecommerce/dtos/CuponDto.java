package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;
import com.nahuel.ecommerce.entitys.TipoMoneda;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class CuponDto {

    private String codigoCupon;
    private String descripcion;
    private AlcanceCupon alcanceCupon;
    private TipoDescuento tipoDescuento;
    private BigDecimal valorDescuento;
    private TipoMoneda moneda;
    private BigDecimal montoMinimoCarrito;
    private Integer limiteUsoPorUsuario;
    private Instant iniciadoEn;
    private Instant terminadoEn;

}
