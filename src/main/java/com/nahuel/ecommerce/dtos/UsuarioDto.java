// dtos/UsuarioDto.java
package com.nahuel.ecommerce.dtos;

import com.nahuel.ecommerce.entitys.Rol;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UsuarioDto {
    private UUID id;
    private String email;
    private String nombreUsuario;
    private Rol rol;
    private Boolean activo;
    private boolean accesoLocal;    // ¿puede entrar con contraseña?
    private boolean accesoGoogle;   // ¿puede entrar con Google?
    private Instant creadoEn;
    // supabaseId y contrasena NUNCA se exponen
}