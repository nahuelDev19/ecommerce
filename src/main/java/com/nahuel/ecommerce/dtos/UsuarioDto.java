package com.nahuel.ecommerce.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class UsuarioDto {

    private UUID id;
    private String nombreUsuario;
    private String contrasena;
    private String email;



}
