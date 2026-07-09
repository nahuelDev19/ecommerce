package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static com.nahuel.ecommerce.entitys.EstadoCarrito.ACTIVO;

@Entity
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private EstadoCarrito EstadoCarrito= ACTIVO;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false )
    private Usuario usuario;


    /*private String moneda;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant ultimaInteraccion;
    private Instant abandonedAt;

     */
    //private Set<ItemCarrito> itemCarritos;

}
