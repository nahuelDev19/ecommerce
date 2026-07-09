package com.nahuel.ecommerce.entitys;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class ItemCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
/*
    private Carrito carrito;
    private Producto producto;
    private Integer cantidad;
    private BigDecimal subTotal;
    private BigDecimal total;
    private Instant creadoEn;
    private Instant actualizadoEn;


 */

}
