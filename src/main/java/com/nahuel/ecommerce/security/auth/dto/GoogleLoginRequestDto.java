package com.nahuel.ecommerce.security.auth.dto;



import jakarta.validation.constraints.NotBlank;

/** El access_token que el frontend recibió de Supabase tras el login con Google. */
public record GoogleLoginRequestDto(@NotBlank String tokenSupabase) {}