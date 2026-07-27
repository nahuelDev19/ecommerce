package com.nahuel.ecommerce.security.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroRequestDto {

    @NotBlank
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    @NotBlank @Size(max = 100)
    private String nombreUsuario;
}
