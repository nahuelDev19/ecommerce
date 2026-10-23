package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_pago_externo_id", columnNames = "pago_externo_id"))
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    private TipoMoneda moneda;

    private String preferenciaId;
    @Column(name = "pago_externo_id")
    private String pagoExternoId;

    private String urlPago;

    private Instant creadoEn;
    private Instant actualizadoEn;
}
