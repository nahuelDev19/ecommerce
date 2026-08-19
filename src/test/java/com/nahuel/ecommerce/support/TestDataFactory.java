package com.nahuel.ecommerce.support;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;

import java.util.UUID;

/**
 * Fábrica de Usuarios de prueba, para usar en tests unitarios y de
 * integración sin depender del flujo real de registro/persistencia.
 */
public class TestDataFactory {

    public static Usuario usuarioCliente() {
        return usuarioConRol(Rol.CLIENTE);
    }

    public static Usuario usuarioAdmin() {
        return usuarioConRol(Rol.ADMIN);
    }

    public static Usuario usuarioPropietario() {
        return usuarioConRol(Rol.PROPIETARIO);
    }

    public static Usuario usuarioConRol(Rol rol) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nombreUsuario("usuario-test-" + rol.name().toLowerCase())
                .email(rol.name().toLowerCase() + "@test.com")
                .contrasena("password-test-123")
                .activo(true)
                .rol(rol)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    /** Variante inactiva, útil para testear isEnabled() == false (login/JWT bloqueado). */
    public static Usuario usuarioInactivo() {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nombreUsuario("usuario-test-inactivo")
                .email("inactivo@test.com")
                .contrasena("password-test-123")
                .activo(false)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    /** Variante registrada vía Google, útil si algún test depende del origen de registro. */
    public static Usuario usuarioGoogle(Rol rol) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nombreUsuario("usuario-test-google-" + rol.name().toLowerCase())
                .email(rol.name().toLowerCase() + ".google@test.com")
                .contrasena("password-test-123")
                .activo(true)
                .rol(rol)
                .origenRegistro(OrigenRegistro.GOOGLE)
                .supabaseId(UUID.randomUUID())
                .build();
    }

    /** Permite override puntual de email, útil cuando el test necesita un valor específico. */
    public static Usuario usuarioConEmail(String email, Rol rol) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nombreUsuario("usuario-test-" + rol.name().toLowerCase())
                .email(email)
                .contrasena("password-test-123")
                .activo(true)
                .rol(rol)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }
}