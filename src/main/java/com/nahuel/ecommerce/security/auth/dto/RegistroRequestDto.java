package com.nahuel.ecommerce.security.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistroRequestDto {

    @NotBlank
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "password no debe ser vació")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
            message = "La contraseña debe tener 8 caracteres, una mayúscula, una minúscula y un número"
    )
    private String contrasena;

    @NotBlank @Size(max = 100)
    private String nombreUsuario;
}
