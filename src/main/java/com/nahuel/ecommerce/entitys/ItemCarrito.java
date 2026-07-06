package com.nahuel.ecommerce.entitys;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ItemCarrito {


    private UUID id;
    private Carrito carrito;
    private Producto producto;
    private Integer cantidad;
    private BigDecimal subTotal;
    private BigDecimal total;
    private Instant creadoEn;
    private Instant actualizadoEn;


}
