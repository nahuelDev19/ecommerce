package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.repositories.OrdenRepository;
import com.nahuel.ecommerce.repositories.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebhookFirmaTest {
    private static final String DATA_ID = "123456789";
    private static final String REQUEST_ID = "request-firma-test";
    private static final String TS = "1700000000";

    private WebhookService servicio;
    private String secretoFalso;

    @BeforeEach
    void preparar() throws Exception {
        try (InputStream archivo = new ClassPathResource("application-test.yml").getInputStream()) {
            Map<String, Object> propiedades = new Yaml().load(archivo);
            secretoFalso = (String) propiedades.get("MERCADOPAGO_WEBHOOK_SECRET");
        }
        assertThat(secretoFalso).isNotBlank();
        servicio = new WebhookService(mock(OrdenRepository.class), mock(PagoRepository.class),
                mock(PasarelaPago.class));
        ReflectionTestUtils.setField(servicio, "webhookSecret", secretoFalso);
    }

    @Test
    void firmaValidaDevuelveTrue() throws Exception {
        String header = "ts=" + TS + ",v1=" + calcularFirma();

        assertThat(servicio.validarFirma(header, REQUEST_ID, DATA_ID)).isTrue();
    }

    @Test
    void firmaAlteradaDevuelveFalse() throws Exception {
        String original = calcularFirma();
        char distinto = original.charAt(0) == '0' ? '1' : '0';
        String alterada = distinto + original.substring(1);
        String header = "ts=" + TS + ",v1=" + alterada;

        assertThat(servicio.validarFirma(header, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void sinHeaderSignatureDevuelveFalse() {
        assertThat(servicio.validarFirma(null, REQUEST_ID, DATA_ID)).isFalse();
    }

    @Test
    void sinTimestampDevuelveFalse() throws Exception {
        String header = "v1=" + calcularFirma();

        assertThat(servicio.validarFirma(header, REQUEST_ID, DATA_ID)).isFalse();
    }

    private String calcularFirma() throws Exception {
        String mensaje = "id:" + DATA_ID + ";request-id:" + REQUEST_ID + ";ts:" + TS + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretoFalso.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(mensaje.getBytes(StandardCharsets.UTF_8)));
    }
}
