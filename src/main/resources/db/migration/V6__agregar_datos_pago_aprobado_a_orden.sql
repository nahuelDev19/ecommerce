ALTER TABLE orden
    ADD COLUMN pago_aprobado_id VARCHAR(255),
    ADD COLUMN pagado_en TIMESTAMPTZ,
    ADD COLUMN vence_en TIMESTAMPTZ;

-- Las órdenes existentes conservan una vigencia de dos horas desde su creación.
-- Si creado_en es NULL, vence_en queda NULL para no inventar una fecha.
UPDATE orden
SET vence_en = creado_en + INTERVAL '2 hours'
WHERE creado_en IS NOT NULL;
