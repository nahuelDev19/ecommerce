package com.nahuel.ecommerce.checkout;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class WebhookService {

    private final String webhookSecret;


    public WebhookService(@Value("${mercadopago.webhook.secret}") String webhookSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalArgumentException("La clave secreta del webhook debe estar configurada");
        }
        this.webhookSecret = webhookSecret;
    }

    public boolean validarFirma(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }

        String timestamp = null;
        String firmaRecibida = null;
        for (String parte : xSignature.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length != 2) {
                continue;
            }
            if ("ts".equals(par[0].trim())) {
                if (timestamp != null) {
                    return false;
                }
                timestamp = par[1].trim();
            } else if ("v1".equals(par[0].trim())) {
                if (firmaRecibida != null) {
                    return false;
                }
                firmaRecibida = par[1].trim();
            }
        }

        if (timestamp == null || !timestamp.matches("[0-9]+")
                || firmaRecibida == null || !firmaRecibida.matches("[0-9a-fA-F]{64}")) {
            return false;
        }

        // Formato del mensaje firmado por Mercado Pago; no es el JSON del aviso.
        StringBuilder mensaje = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) {
            mensaje.append("id:").append(dataId.trim()).append(';');
        }
        if (xRequestId != null && !xRequestId.isBlank()) {
            mensaje.append("request-id:").append(xRequestId.trim()).append(';');
        }
        mensaje.append("ts:").append(timestamp).append(';');

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] firmaEsperada = mac.doFinal(mensaje.toString().getBytes(StandardCharsets.UTF_8));
            byte[] firmaRecibidaBytes = HexFormat.of().parseHex(firmaRecibida);
            return MessageDigest.isEqual(firmaEsperada, firmaRecibidaBytes);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", ex);
        }
    }
}
