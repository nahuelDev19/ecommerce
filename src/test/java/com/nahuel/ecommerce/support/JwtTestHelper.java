package com.nahuel.ecommerce.support;

import com.nahuel.ecommerce.entitys.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.yaml.snakeyaml.Yaml;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Helper de test para GENERAR tokens JWT "de mentira", pensados
 * exclusivamente para tests de seguridad (@WebMvcTest).
 *
 * IMPORTANTE:
 * - No depende de JwtService ni de ninguna clase de producción: construye
 *   el token con su propia lógica (jjwt).
 * - El secret/expiration se leen de application-test.yml en tiempo de
 *   ejecución (no están hardcodeados), para no desincronizarse si cambia
 *   el YAML. Esto es leer un VALOR DE CONFIGURACIÓN, no importar lógica
 *   de producción.
 * - Solo GENERA tokens. Para validar/inspeccionar tokens de test, usar
 *   JwtTestTokenValidator — una clase separada, sin código compartido
 *   con esta (cada una lee y arma su propia clave de forma independiente).
 */
public class JwtTestHelper {

    private static final SecretKey TEST_SIGNING_KEY = cargarClaveDesdeYaml();
    private static final long EXPIRATION_MS = cargarExpiracionDesdeYaml();

    /** Token válido, con expiración según config de test, para el usuario dado. */
    public String tokenParaUsuario(Usuario usuario) {
        return construirToken(
                usuario.getUsername(),
                usuario.getRol().name(),
                new Date(System.currentTimeMillis() + EXPIRATION_MS),
                TEST_SIGNING_KEY
        );
    }

    /** Token ya vencido, útil para testear el camino de expiración (401). */
    public String tokenExpirado(Usuario usuario) {
        return construirToken(
                usuario.getUsername(),
                usuario.getRol().name(),
                new Date(System.currentTimeMillis() - 1000),
                TEST_SIGNING_KEY
        );
    }

    /** Token con firma inválida (clave distinta), útil para testear 401 por firma. */
    public String tokenConFirmaInvalida(Usuario usuario) {
        SecretKey otraClave = Keys.hmacShaKeyFor(
                "otra-clave-completamente-distinta-solo-para-test-9876543210".getBytes()
        );
        return construirToken(
                usuario.getUsername(),
                usuario.getRol().name(),
                new Date(System.currentTimeMillis() + EXPIRATION_MS),
                otraClave
        );
    }

    private String construirToken(String username, String rol, Date expiracion, SecretKey clave) {
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    /**
     * RequestPostProcessor listo para usar en MockMvc:
     * mockMvc.perform(get("/api/cupones").with(jwt.bearerToken(usuarioAdmin)))
     */
    public RequestPostProcessor bearerToken(Usuario usuario) {
        String token = tokenParaUsuario(usuario);
        return request -> {
            request.addHeader("Authorization", "Bearer " + token);
            return request;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> leerSeccionJwt() {
        try (InputStream in = new ClassPathResource("application-test.yml").getInputStream()) {
            Map<String, Object> yaml = new Yaml().load(in);
            return (Map<String, Object>) yaml.get("jwt");
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer application-test.yml para configurar JwtTestTokenGenerator", e);
        }
    }

    private static SecretKey cargarClaveDesdeYaml() {
        String secretBase64 = (String) leerSeccionJwt().get("secret");
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBase64));
    }

    private static long cargarExpiracionDesdeYaml() {
        Object expiration = leerSeccionJwt().get("expiration");
        return Long.parseLong(String.valueOf(expiration));
    }
}