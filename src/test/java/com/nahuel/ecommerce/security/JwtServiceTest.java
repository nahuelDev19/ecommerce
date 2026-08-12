package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            Base64.getEncoder().encodeToString(
                    "esta-es-una-clave-secreta-de-32-bytes!!"
                            .getBytes()
            );

    private static final long EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                SECRET
        );

        ReflectionTestUtils.setField(
                jwtService,
                "expiration",
                EXPIRATION
        );
    }

    // =========================================================
    // generateToken
    // =========================================================

    @Test
    void generateToken_deberiaGenerarTokenValido() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token = jwtService.generateToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
                usuarioId.toString(),
                jwtService.extractSubject(token)
        );
    }

    @Test
    void generateToken_deberiaGuardarIdComoSubject() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token = jwtService.generateToken(usuario);

        String subject = jwtService.extractSubject(token);

        assertEquals(usuarioId.toString(), subject);
    }

    @Test
    void generateToken_deberiaCrearFechaDeExpiracionPosteriorALaActual() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        Date antes = new Date();

        String token = jwtService.generateToken(usuario);

        Date expiration = jwtService.extractClaim(
                token,
                Claims::getExpiration
        );

        Date despues = new Date();

        assertTrue(expiration.after(antes));
        assertTrue(expiration.after(despues));
    }

    // =========================================================
    // extractSubject
    // =========================================================

    @Test
    void extractSubject_deberiaDevolverSubjectDelToken() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token = jwtService.generateToken(usuario);

        String subject = jwtService.extractSubject(token);

        assertEquals(usuarioId.toString(), subject);
    }

    // =========================================================
    // extractClaim
    // =========================================================

    @Test
    void extractClaim_deberiaExtraerClaimSolicitado() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token = jwtService.generateToken(usuario);

        Date expiration = jwtService.extractClaim(
                token,
                Claims::getExpiration
        );

        assertNotNull(expiration);
    }

    @Test
    void extractClaim_deberiaPermitirObtenerSubject() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token = jwtService.generateToken(usuario);

        String subject = jwtService.extractClaim(
                token,
                Claims::getSubject
        );

        assertEquals(usuarioId.toString(), subject);
    }

    // =========================================================
    // isTokenValid
    // =========================================================

    @Test
    void isTokenValid_deberiaRetornarTrueSiTokenPerteneceAlUsuarioYNoEstaExpirado() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token = jwtService.generateToken(usuario);

        boolean resultado =
                jwtService.isTokenValid(token, usuario);

        assertTrue(resultado);
    }

    @Test
    void isTokenValid_deberiaRetornarFalseSiTokenPerteneceAOtroUsuario() {

        UUID usuarioIdToken = UUID.randomUUID();
        UUID usuarioIdActual = UUID.randomUUID();

        Usuario usuarioDelToken = Usuario.builder()
                .id(usuarioIdToken)
                .build();

        Usuario usuarioActual = Usuario.builder()
                .id(usuarioIdActual)
                .build();

        String token =
                jwtService.generateToken(usuarioDelToken);

        boolean resultado =
                jwtService.isTokenValid(token, usuarioActual);

        assertFalse(resultado);
    }

    @Test
    void isTokenValid_deberiaRetornarFalseSiTokenEstaExpirado() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(SECRET)
        );

        Date ahora = new Date();

        String token = Jwts.builder()
                .subject(usuarioId.toString())
                .issuedAt(
                        new Date(ahora.getTime() - 10000)
                )
                .expiration(
                        new Date(ahora.getTime() - 1000)
                )
                .signWith(key)
                .compact();

        boolean resultado =
                jwtService.isTokenValid(token, usuario);

        assertFalse(resultado);
    }

    // =========================================================
    // getExpirationInSeconds
    // =========================================================

    @Test
    void getExpirationInSeconds_deberiaConvertirMilisegundosASegundos() {

        long resultado =
                jwtService.getExpirationInSeconds();

        assertEquals(3600L, resultado);
    }

    // =========================================================
    // tokens inválidos
    // =========================================================

    @Test
    void extractSubject_deberiaLanzarExcepcionSiTokenEsInvalido() {

        String token = "token-invalido";

        assertThrows(
                RuntimeException.class,
                () -> jwtService.extractSubject(token)
        );
    }

    @Test
    void extractSubject_deberiaLanzarExcepcionSiTokenEstaManipulado() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token =
                jwtService.generateToken(usuario);

        String tokenManipulado =
                token.substring(0, token.length() - 2) + "xx";

        assertThrows(
                RuntimeException.class,
                () -> jwtService.extractSubject(tokenManipulado)
        );
    }

    // =========================================================
    // casos null
    // =========================================================

    @Test
    void generateToken_deberiaLanzarExcepcionSiUsuarioEsNull() {

        assertThrows(
                NullPointerException.class,
                () -> jwtService.generateToken(null)
        );
    }

    @Test
    void isTokenValid_deberiaLanzarExcepcionSiUsuarioEsNull() {

        UUID usuarioId = UUID.randomUUID();

        Usuario usuario = Usuario.builder()
                .id(usuarioId)
                .build();

        String token =
                jwtService.generateToken(usuario);

        assertThrows(
                NullPointerException.class,
                () -> jwtService.isTokenValid(token, null)
        );
    }

    @Test
    void isTokenValid_deberiaLanzarExcepcionSiTokenEsNull() {

        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .build();

        assertThrows(
                RuntimeException.class,
                () -> jwtService.isTokenValid(null, usuario)
        );
    }
}