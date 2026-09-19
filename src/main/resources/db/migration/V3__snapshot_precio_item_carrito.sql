ALTER TABLE item_carrito
    ADD COLUMN precio_agregado_en TIMESTAMPTZ;


UPDATE item_carrito
SET precio_agregado_en = COALESCE(actualizado_en, now())
WHERE precio_agregado_en IS NULL;