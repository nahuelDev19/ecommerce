package com.nahuel.ecommerce.entitys;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor @AllArgsConstructor @Data
public class Cupon {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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
