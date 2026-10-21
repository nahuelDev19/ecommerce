ALTER TABLE orden
    ADD COLUMN pago_aprobado_id VARCHAR(255),
    ADD COLUMN pagado_en TIMESTAMPTZ;
