package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {


    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;


    public String generateToken(Usuario usuario,Date expirationDate) {

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .issuedAt(new Date())
                .expiration(expirationDate)
                .signWith(getSignKey())
                .compact();
    }

    public String generateToken(Usuario usuario) {
        return generateToken(usuario, new Date(System.currentTimeMillis() + expiration));
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        try {
        return Jwts.parser()
                .verifyWith(getSignKey())   // usamos la misma clave para verificar
                .build()
                .parseSignedClaims(token)
                .getPayload();
        }catch (ExpiredJwtException e){
            return e.getClaims();
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token,Usuario usuario) {
        final String subjectId= extractSubject(token);
        return subjectId.equals(usuario.getId().toString()) && !isTokenExpired(token);

    }

    public long getExpirationInSeconds() {
        return expiration / 1000;
    }
}
