-- Completa con 'CLIENTE' las filas que quedaron sin rol asignado
UPDATE usuario
SET rol = 'CLIENTE'
WHERE rol IS NULL;

-- Asegura que la columna tenga el default y la restricción NOT NULL
ALTER TABLE usuario
ALTER COLUMN rol SET DEFAULT 'CLIENTE';

ALTER TABLE usuario
ALTER COLUMN rol SET NOT NULL;