package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {


    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final UUID id;
        try {
            id = UUID.fromString(jwtService.extractSubject(jwt));

        } catch (JwtException | IllegalArgumentException e) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
        return;
    }

        if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Usuario usuario= usuarioRepository.findById(id).orElseThrow(()-> new  RuntimeException("Usuario no encontrado"));

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(usuario.getEmail());


            if (jwtService.isTokenValid(jwt, usuario)) {


                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));


                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Token inválido"
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
