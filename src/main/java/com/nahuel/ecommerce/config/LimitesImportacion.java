package com.nahuel.ecommerce.config;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "importacion")
public class LimitesImportacion {


    @NotNull
    private DataSize maxTamanioBytes;
    private int maxFilas;
    private int maxColumnas;
    private int maxLargoCelda;
    @DecimalMin("0.001")
    @DecimalMax("1.0")
    private float minRatioDescompresion;
    private int tamanioLote;
    private String directorioTemporal;
    private long maxTextoDescomprimido;




}
