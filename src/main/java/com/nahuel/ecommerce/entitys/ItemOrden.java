package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ItemOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Column(nullable = false, updatable = false)
    private UUID productoId;

    // Copias de la compra: no se consultan los valores actuales del catalogo.
    @Column(nullable = false, updatable = false)
    private String nombreProducto;

    @Column(nullable = false,updatable = false)
    private BigDecimal precioUnitario;

    private Integer cantidad;

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
