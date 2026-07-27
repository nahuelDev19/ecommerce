package com.nahuel.ecommerce.security.auth.dto;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import lombok.Builder;

@Builder
public record TokenResponseDto(
        String token,
        String tipo,          // siempre "Bearer"
        long expiraEnSegundos,
        UsuarioDto usuario    // así el frontend no necesita otra llamada
) {}