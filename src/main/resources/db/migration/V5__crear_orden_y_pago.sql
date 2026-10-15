-- Pendiente: definir las tablas de orden, item de orden y pago.
-- V5__crear_orden_y_pago.sql

CREATE TABLE orden (
    id               UUID PRIMARY KEY,
    usuario_id       UUID NOT NULL,
    estado           VARCHAR(255) NOT NULL,
    subtotal         NUMERIC(38, 2),
    descuento_total  NUMERIC(38, 2),
    total            NUMERIC(38, 2),
    moneda           VARCHAR(255),
    creado_en        TIMESTAMPTZ,
    actualizado_en   TIMESTAMPTZ,

    CONSTRAINT fk_orden_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),

    CONSTRAINT chk_orden_estado
        CHECK (estado IN ('PENDIENTE_PAGO', 'PAGADA', 'CANCELADA'))
);

CREATE TABLE item_orden (
    id               UUID PRIMARY KEY,
    orden_id         UUID NOT NULL,
    producto_id      UUID NOT NULL,
    nombre_producto  VARCHAR(255) NOT NULL,
    precio_unitario  NUMERIC(38, 2),
    cantidad         INTEGER,

    CONSTRAINT fk_item_orden_orden
        FOREIGN KEY (orden_id) REFERENCES orden (id),

    CONSTRAINT chk_item_orden_precio
        CHECK (precio_unitario >= 0),

    CONSTRAINT chk_item_orden_cantidad
        CHECK (cantidad > 0)
);

CREATE TABLE pago (
    id               UUID PRIMARY KEY,
    orden_id         UUID NOT NULL,
    estado           VARCHAR(255) NOT NULL,
    monto            NUMERIC(38, 2),
    moneda           VARCHAR(255),
    preferencia_id   VARCHAR(255),
    pago_externo_id  VARCHAR(255),
    url_pago         VARCHAR(255),
    creado_en        TIMESTAMPTZ,
    actualizado_en   TIMESTAMPTZ,

    CONSTRAINT fk_pago_orden
        FOREIGN KEY (orden_id) REFERENCES orden (id),

    CONSTRAINT uq_pago_externo_id
        UNIQUE (pago_externo_id),

    CONSTRAINT chk_pago_estado
        CHECK (
            estado IN (
                'PENDIENTE',
                'EN_PROCESO',
                'APROBADO',
                'RECHAZADO',
                'CANCELADO'
            )
        ),

    CONSTRAINT chk_pago_monto
        CHECK (monto >= 0)
);

CREATE INDEX idx_orden_usuario
    ON orden (usuario_id);

CREATE INDEX idx_item_orden_orden
    ON item_orden (orden_id);

CREATE INDEX idx_pago_orden
    ON pago (orden_id);