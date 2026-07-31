package com.nahuel.ecommerce.security;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class SupabaseTokenVerifier {

    private final JwtDecoder decoder;

    public SupabaseTokenVerifier(@Value("${supabase.url}") String supabaseUrl) {
        String issuer = supabaseUrl + "/auth/v1";

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder
                .withJwkSetUri(supabaseUrl + "/auth/v1/.well-known/jwks.json")
                .build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
        nimbusDecoder.setJwtValidator(validator);

        this.decoder = nimbusDecoder;    }

    public Jwt verificar(String token){
        if(token ==null || token.isBlank()){
            throw new RuntimeException("token no informado");
        }
        try {
            return decoder.decode(token);
        } catch (JwtException e) {
            //throw new TokenInvalidoException("Token inválido", e);
            throw new RuntimeException("Token inválido", e);
        }
    }



}


