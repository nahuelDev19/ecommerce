package com.nahuel.ecommerce.entitys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado = EstadoOrden.PENDIENTE_PAGO;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL)
    private List<ItemOrden> items = new ArrayList<>();

    private BigDecimal subtotal;

    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    private BigDecimal total;
    // ID externo del pago aprobado que pagó esta orden.
    @Column(name = "pago_aprobado_id")
    private String paymentId;

    @Enumerated(EnumType.STRING)
    private TipoMoneda moneda;

    private Instant creadoEn;
    @Column(name = "vence_en")
    private Instant venceEn;
    @Column(name = "pagado_en")
    private Instant pagadoEn;
    private Instant actualizadoEn;
}
