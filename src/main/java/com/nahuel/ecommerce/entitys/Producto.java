package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nombre;

    private String descripcion;

    private BigDecimal precioBase;

    private String moneda;

    private Boolean activo;

    private Instant fechaDescontinuado;

    private Instant fechaCreacion;

    private Instant fechaActualizacion;


}
