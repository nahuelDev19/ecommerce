package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
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
    @Column(nullable = false, unique = true)
    private String codigoCupon;
    @Enumerated(EnumType.STRING)
    private EstadoCupon estadoCupon=EstadoCupon.ACTIVO;
    @Enumerated(EnumType.STRING)
    private AlcanceCupon alcanceCupon;
    @Enumerated(EnumType.STRING)
    private TipoDescuento tipoDescuento;
    @Column(nullable = false)
    private BigDecimal valorDescuento;
    @Enumerated(EnumType.STRING)
    private TipoMoneda moneda;
    private BigDecimal montoMinimoCarrito;
    private Integer limiteUsoPorUsuario=1;
    private String descripcion;
    @Column(nullable = false)
    private Instant iniciadoEn;
    @Column(nullable = false)
    private Instant terminaEn;

    public Cupon(String codigoCupon, String descripcion, EstadoCupon estadoCupon, AlcanceCupon alcanceCupon,
                 TipoDescuento tipoDescuento, BigDecimal valorDescuento, TipoMoneda moneda,
                 BigDecimal montoMinimoCarrito, Integer limiteUsoPorUsuario, Instant iniciadoEn, Instant terminaEn) {

        this.codigoCupon = codigoCupon;
        this.descripcion = descripcion;
        this.estadoCupon = estadoCupon;
        this.alcanceCupon = alcanceCupon;
        this.tipoDescuento = tipoDescuento;
        this.valorDescuento = valorDescuento;
        this.moneda = moneda;
        this.montoMinimoCarrito = montoMinimoCarrito;
        this.limiteUsoPorUsuario = limiteUsoPorUsuario;
        this.iniciadoEn = iniciadoEn;
        this.terminaEn = terminaEn;
    }

}
