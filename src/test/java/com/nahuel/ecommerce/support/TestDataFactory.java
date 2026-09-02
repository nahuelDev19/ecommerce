package com.nahuel.ecommerce.support;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;

import java.util.UUID;

/**
 * Fábrica de Usuarios de prueba, para usar en tests unitarios y de
 * integración sin depender del flujo real de registro/persistencia.
 *
 * NOTA: Utiliza UUIDs deterministas y fijos para garantizar repetibilidad
 * en las aserciones y mensajes de error legibles durante las pruebas.
 */
public class TestDataFactory {

    public static final UUID ID_ANA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID ID_BETO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID ID_ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID ID_INACTIVO = UUID.fromString("00000000-0000-0000-0000-000000000004");

    /** Usuario cliente estándar activo (Ana). */
    public static Usuario ana() {
        return Usuario.builder()
                .id(ID_ANA)
                .nombreUsuario("ana-cliente")
                .email("ana@test.com")
                .contrasena("password-test-123")
                .activo(true)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    /** Usuario propietario o cliente secundario activo (Beto). */
    public static Usuario beto() {
        return Usuario.builder()
                .id(ID_BETO)
                .nombreUsuario("beto-propietario")
                .email("beto@test.com")
                .contrasena("password-test-123")
                .activo(true)
                .rol(Rol.PROPIETARIO)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    /** Usuario administrador activo. */
    public static Usuario admin() {
        return Usuario.builder()
                .id(ID_ADMIN)
                .nombreUsuario("admin-user")
                .email("admin@test.com")
                .contrasena("password-test-123")
                .activo(true)
                .rol(Rol.ADMIN)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    /** Variante inactiva, útil para testear cuentas bloqueadas o 401. */
    public static Usuario inactivo() {
        return Usuario.builder()
                .id(ID_INACTIVO)
                .nombreUsuario("usuario-inactivo")
                .email("inactivo@test.com")
                .contrasena("password-test-123")
                .activo(false)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    // --- Métodos de compatibilidad / especificadores puntuales ---

    public static Usuario usuarioCliente() {
        return ana();
    }

    public static Usuario usuarioAdmin() {
        return admin();
    }

    public static Usuario usuarioPropietario() {
        return beto();
    }

    public static Usuario usuarioInactivo() {
        return inactivo();
    }

    /** Variante registrada vía Google con ID determinista basado en el rol. */
    public static Usuario usuarioGoogle(Rol rol) {
        UUID idGoogle = UUID.nameUUIDFromBytes(("google-" + rol.name()).getBytes());
        return Usuario.builder()
                .id(idGoogle)
                .nombreUsuario("usuario-google-" + rol.name().toLowerCase())
                .email(rol.name().toLowerCase() + ".google@test.com")
                .contrasena("password-test-123")
                .activo(true)
                .rol(rol)
                .origenRegistro(OrigenRegistro.GOOGLE)
                .supabaseId(idGoogle)
                .build();
    }

    /** Permite override puntual de email con ID determinista derivado del email. */
    public static Usuario usuarioConEmail(String email, Rol rol) {
        return Usuario.builder()
                .id(UUID.nameUUIDFromBytes(email.getBytes()))
                .nombreUsuario("usuario-test-" + rol.name().toLowerCase())
                .email(email)
                .contrasena("password-test-123")
                .activo(true)
                .rol(rol)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }
}