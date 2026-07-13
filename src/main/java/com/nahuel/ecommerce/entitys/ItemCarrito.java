package com.nahuel.ecommerce.entitys;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
public class ItemCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id")
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    private BigDecimal precioUnitario;




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
