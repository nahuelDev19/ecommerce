package com.nahuel.ecommerce.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "importacion")
public class LimitesImportacion {


    private DataSize maxTamanioBytes;
    private int maxFilas;
    private int maxColumnas;
    private int maxLargoCelda;
    private float minRatioDescompresion;
    private int tamanioLote;
    private String directorioTemporal;





}
