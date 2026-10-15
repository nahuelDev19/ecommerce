package com.nahuel.ecommerce.config;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigAdmin implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${PASS_ADMIN:}")
    private String passAdmin;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;


    @Override
    public void run(String... args) {
        if (passAdmin == null || passAdmin.isBlank()) {
            log.warn("PASS_ADMIN no está definida. No se creará el usuario administrador.");
            return;
        }

        Usuario admin = usuarioRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            admin = Usuario.builder()
                    .nombreUsuario("Admin")
                    .email(adminEmail)
                    .contrasena(passwordEncoder.encode(passAdmin))
                    .rol(Rol.ADMIN)
                    .activo(true)
                    .origenRegistro(OrigenRegistro.LOCAL)
                    .build();

            usuarioRepository.save(admin);
            log.info("Usuario administrador '{}' creado correctamente.", adminEmail);
            return;
        }

        // La fila ya existía (p.ej. creada por la versión anterior). Repara los
        // campos que quedaron en NULL, que son la causa del 401/500 en el login.
        boolean reparado = false;
        if (admin.getActivo() == null) {
            admin.setActivo(true);
            reparado = true;
        }
        if (admin.getOrigenRegistro() == null) {
            admin.setOrigenRegistro(OrigenRegistro.LOCAL);
            reparado = true;
        }

        if (reparado) {
            usuarioRepository.save(admin);
            log.info("Administrador '{}' ya existía; se repararon sus campos NULL.", adminEmail);
        } else {
            log.info("El administrador '{}' ya existe, no se vuelve a insertar.", adminEmail);
        }
    }
}
