-- V4: crea la tabla de trabajos de importación.
-- Numerada V4 (y no V3) porque ese número ya está usado en la rama del snapshot de precios.

CREATE TABLE importacion_job (
    id                    UUID PRIMARY KEY,
    nombre_archivo        VARCHAR(255),
    ruta_archivo_temporal VARCHAR(255),
    huella_contenido      VARCHAR(255),
    solo_validacion       BOOLEAN NOT NULL,
    estado                VARCHAR(255),
    usuario_id            UUID NOT NULL,
    cantidad_filas        INTEGER NOT NULL,
    productos_importados  INTEGER NOT NULL,
    filas_con_error       INTEGER NOT NULL,
    detalle_errores       TEXT,
    mensaje_error         VARCHAR(255),
    fecha_creacion        TIMESTAMP,
    fecha_finalizacion    TIMESTAMP,

    CONSTRAINT fk_importacion_job_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

-- Índice para listar los trabajos de un usuario.
CREATE INDEX idx_importacion_job_usuario
    ON importacion_job (usuario_id);

-- Índice para buscar por la huella del contenido (deduplicación).
CREATE INDEX idx_importacion_job_huella_contenido
    ON importacion_job (huella_contenido);
