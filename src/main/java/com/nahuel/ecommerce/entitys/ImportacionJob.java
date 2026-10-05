package com.nahuel.ecommerce.entitys;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportacionJob {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String nombreArchivo;

    private String rutaArchivoTemporal;

    private String huellaContenido;

    private boolean soloValidacion;

    @Enumerated(EnumType.STRING)
    private EstadoImportacion estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private int cantidadFilas;

    private int productosImportados;

    private int filasConError;

    private String detalleErrores;

    private String mensajeError;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaFinalizacion;

    @PrePersist
    protected void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }



/*
Completar la clase ImportacionJob, que hoy está vacía.
Es la entidad que representa un trabajo de importación y
tiene que guardar: el id, el nombre del archivo que subió
el usuario, la ruta del archivo temporal, la  (ver punto 9), si es una corrida de sólo
validación (ver punto 11), en qué estado está, qué usuario
lo pidió, cuántas filas tenía el archivo, cuántos productos
se importaron, cuántas filas tuvieron error, el detalle de
esos errores, un mensaje corto si falló, y las fechas de
creación y finalización.

 */
}
