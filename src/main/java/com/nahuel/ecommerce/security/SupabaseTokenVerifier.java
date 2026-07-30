package com.nahuel.ecommerce.security;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class SupabaseTokenVerifier {

    private final JwtDecoder decoder;

    public SupabaseTokenVerifier(@Value("${supabase.url}") String supabaseUrl) {
        this.decoder = NimbusJwtDecoder.withJwkSetUri(supabaseUrl + "/auth/v1/.well-known/jwks.json").build();
    }

    public Jwt verificar(String token){
        return decoder.decode(token);
    }



}


