package com.nahuel.ecommerce.entitys;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import static com.nahuel.ecommerce.entitys.EstadoCarrito.ACTIVO;

public class Carrito {

    private UUID id;
    private EstadoCarrito EstadoCarrito= ACTIVO;
    private UUID usuarioId;
    private String moneda;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant ultimaInteraccion;
    private Instant abandonedAt;
    private Set<ItemCarrito> itemCarritos;

}
