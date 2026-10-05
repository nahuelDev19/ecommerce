package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.EstadoImportacion;
import com.nahuel.ecommerce.entitys.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ImportacionJobDto {


    private String rutaArchivoTemporal;
    private EstadoImportacion estado;
    private int cantidadFilas;
    private int productosImportados;
    private int filasConError;
    private String detalleErrores;
    private String mensajeError;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaFinalizacion;
}
