package com.nahuel.ecommerce.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UsuarioDto {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 30,
            message = "El nombre de usuario debe tener entre 3 y 30 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100,
            message = "La contraseña debe tener al menos 8 caracteres")
    private String contrasena;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 100,
            message = "El email no puede superar los 100 caracteres")
    private String email;


}
