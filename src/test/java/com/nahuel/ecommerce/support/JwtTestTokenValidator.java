package com.nahuel.ecommerce.support;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

/**
 * Helper de test para VALIDAR/inspeccionar tokens JWT de test.
 *
 * IMPORTANTE — separación de responsabilidades:
 * - Esta clase NO genera tokens (eso es responsabilidad exclusiva de
 *   JwtTestTokenGenerator). No la extiende, no la compone, no reusa
 *   ninguno de sus métodos ni construye la clave de forma compartida.
 * - No depende de JwtService: parsea el token directamente con jjwt.
 * - Uso típico: aserciones sobre claims de un token ya generado (por
 *   ejemplo, verificar que el claim "rol" tiene el valor esperado), o
 *   testear el propio JwtTestTokenGenerator si alguna vez se decide
 *   agregarle tests.
 */
public class JwtTestTokenValidator {

    private static final SecretKey TEST_VERIFYING_KEY = cargarClaveDesdeYaml();

    /**
     * Parsea y valida el token. Si es inválido (firma incorrecta,
     * expirado, malformado), tira JwtException — dejá que el test
     * la capture con assertThrows si es lo que querés probar.
     */
    public Claims parsearClaims(String token) {
        return Jwts.parser()
                .verifyWith(TEST_VERIFYING_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** true si el token es válido (firma correcta y no expirado); false si no. */
    public boolean esValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extraerUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    public String extraerRol(String token) {
        return parsearClaims(token).get("rol", String.class);
    }

    @SuppressWarnings("unchecked")
    private static SecretKey cargarClaveDesdeYaml() {
        try (InputStream in = new ClassPathResource("application-test.yml").getInputStream()) {
            Map<String, Object> yaml = new Yaml().load(in);
            Map<String, Object> jwt = (Map<String, Object>) yaml.get("jwt");
            String secretBase64 = (String) jwt.get("secret");
            return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer jwt.secret desde application-test.yml", e);
        }
    }
}