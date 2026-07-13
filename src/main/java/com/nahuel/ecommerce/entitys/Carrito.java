package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static com.nahuel.ecommerce.entitys.EstadoCarrito.ACTIVO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private EstadoCarrito estadoCarrito= ACTIVO;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false )
    private Usuario usuario;


    @OneToMany(mappedBy = "carrito")
    private Set<ItemCarrito> items = new HashSet<>();

    //private BigDecimal subtotal;
    //private BigDecimal descuentoTotal;
    //private BigDecimal total;
    private Instant creadoEn;
    private Instant actualizadoEn;
    private Instant ultimaInteraccion;

    /*
    private String moneda;
    private Set<ItemCarrito> itemCarritos;
     */

}
