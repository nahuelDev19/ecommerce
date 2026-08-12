package com.nahuel.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupabaseTokenVerifierTest {

    private SupabaseTokenVerifier verifier;

    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {

        verifier = new SupabaseTokenVerifier(
                "https://supabase.example.com"
        );

        decoder = mock(JwtDecoder.class);

        ReflectionTestUtils.setField(
                verifier,
                "decoder",
                decoder
        );
    }

    // =========================================================
    // verificar - happy path
    // =========================================================

    @Test
    void verificar_deberiaDevolverJwtCuandoTokenEsValido() {

        String token = "token-valido";

        Jwt jwt = mock(Jwt.class);

        when(decoder.decode(token))
                .thenReturn(jwt);

        Jwt resultado = verifier.verificar(token);

        assertNotNull(resultado);
        assertSame(jwt, resultado);

        verify(decoder).decode(token);
    }

    // =========================================================
    // token null
    // =========================================================

    @Test
    void verificar_deberiaLanzarExcepcionSiTokenEsNull() {

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> verifier.verificar(null)
        );

        assertEquals(
                "token no informado",
                exception.getMessage()
        );

        verifyNoInteractions(decoder);
    }

    // =========================================================
    // token vacío
    // =========================================================

    @Test
    void verificar_deberiaLanzarExcepcionSiTokenEstaVacio() {

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> verifier.verificar("")
        );

        assertEquals(
                "token no informado",
                exception.getMessage()
        );

        verifyNoInteractions(decoder);
    }

    // =========================================================
    // token solamente espacios
    // =========================================================

    @Test
    void verificar_deberiaLanzarExcepcionSiTokenTieneSoloEspacios() {

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> verifier.verificar("   ")
        );

        assertEquals(
                "token no informado",
                exception.getMessage()
        );

        verifyNoInteractions(decoder);
    }

    // =========================================================
    // token inválido
    // =========================================================

    @Test
    void verificar_deberiaLanzarExcepcionSiJwtDecoderLanzaJwtException() {

        String token = "token-invalido";

        when(decoder.decode(token))
                .thenThrow(new JwtException("firma inválida"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> verifier.verificar(token)
        );

        assertEquals(
                "Token inválido",
                exception.getMessage()
        );

        assertInstanceOf(
                JwtException.class,
                exception.getCause()
        );

        verify(decoder).decode(token);
    }

    // =========================================================
    // propagación de la causa original
    // =========================================================

    @Test
    void verificar_deberiaConservarLaExcepcionOriginalComoCause() {

        JwtException causa =
                new JwtException("token expirado");

        when(decoder.decode("token"))
                .thenThrow(causa);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> verifier.verificar("token")
        );

        assertSame(
                causa,
                exception.getCause()
        );
    }

    // =========================================================
    // token válido con contenido
    // =========================================================

    @Test
    void verificar_deberiaDevolverJwtSinModificarlo() {

        String token = "token-valido";

        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("usuario-123")
                .claim("email", "nahuel@gmail.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(decoder.decode(token))
                .thenReturn(jwt);

        Jwt resultado = verifier.verificar(token);

        assertSame(jwt, resultado);

        assertEquals(
                "usuario-123",
                resultado.getSubject()
        );

        assertEquals(
                "nahuel@gmail.com",
                resultado.getClaimAsString("email")
        );
    }
}