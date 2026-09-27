package com.nahuel.ecommerce.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class PoiSecurityConfig {

    private final LimitesImportacion limitesImportacion;

    @PostConstruct
    public void configurarLimitesPoi() {
        // POI exime del chequeo de ratio a TODA entrada que descomprima por debajo de
        // 100KB (su GRACE_ENTRY_SIZE). Verificado: una entrada de 90KB con ratio 0.0007
        // pasa sin ser detectada. Bajarlo a 8KB cierra ese hueco.
        ZipSecureFile.setGraceEntrySize(8 * 1024L);

        // Tope de cantidad de entradas del zip (POI trae 1000). Sin esto, la bomba de
        // muchas entradas chicas sigue siendo posible.
        ZipSecureFile.setMaxFileCount(500L);
        ZipSecureFile.setMinInflateRatio(
                limitesImportacion.getMinRatioDescompresion()
        );

        ZipSecureFile.setMaxEntrySize(
                limitesImportacion.getMaxTamanioBytes().toBytes()
        );

        ZipSecureFile.setMaxTextSize(
                limitesImportacion.getMaxTextoDescomprimido()
        );

    }
}
