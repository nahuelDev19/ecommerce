package com.nahuel.ecommerce.security.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@AllArgsConstructor @NoArgsConstructor
@Data
public class LoginRequestDto {


    @NotBlank(message = "username no debe ser vació")
    @Email(message = "dormato de email no aceptado")
    private String email;
    @NotBlank(message = "password no debe ser vació")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
            message = "La contraseña debe tener 8 caracteres, una mayúscula, una minúscula y un número"
    )
    private String contrasena;

}