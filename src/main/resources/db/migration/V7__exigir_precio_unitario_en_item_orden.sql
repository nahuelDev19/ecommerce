-- Complementa nullable = false de ItemOrden en bases existentes.
-- Si existen precios NULL, la migración falla sin inventar precios históricos:
-- esos datos deben revisarse antes de volver a ejecutarla.
ALTER TABLE item_orden
    ALTER COLUMN precio_unitario SET NOT NULL;

-- updatable = false controla los UPDATE de Hibernate; no es una restricción SQL.
