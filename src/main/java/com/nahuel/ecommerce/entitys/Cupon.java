package com.nahuel.ecommerce.entitys;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Cupon {
    private UUID id;
    private String codigoCupon; //unico
    private Boolean activo;
    private AlcanceCupon alcanceCupon ;//(POR_ITEM / POR_CARRITO)
    private TipoDescuento tipoDescuento;
    private BigDecimal valorDescuento;//(ej. 10.00 o 15%)
    private String moneda;
    private BigDecimal montoMinimoCarrito;
    private Integer limiteUsoTotal;
    private Integer limiteUsoPorUsuario;
    private Instant creadoEn;


}
