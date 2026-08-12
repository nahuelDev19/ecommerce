package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImpTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImp userDetailsService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .email("nahuel@gmail.com")
                .nombreUsuario("Nahuel")
                .contrasena("password-encoded")
                .activo(true)
                .rol(Rol.CLIENTE)
                .origenRegistro(OrigenRegistro.LOCAL)
                .build();
    }

    // =========================================================
    // HAPPY PATH
    // =========================================================

    @Test
    void loadUserByUsername_deberiaDevolverUsuarioCuandoExiste() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        UserDetails resultado =
                userDetailsService.loadUserByUsername("nahuel@gmail.com");

        assertNotNull(resultado);
        assertSame(usuario, resultado);

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    // =========================================================
    // USUARIO NO EXISTE
    // =========================================================

    @Test
    void loadUserByUsername_deberiaLanzarExcepcionSiUsuarioNoExiste() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "nahuel@gmail.com"
                )
        );

        assertEquals(
                "email del usuario no encontrado nahuel@gmail.com",
                exception.getMessage()
        );

        verify(usuarioRepository)
                .findByEmail("nahuel@gmail.com");
    }

    // =========================================================
    // EMAIL
    // =========================================================

    @Test
    void loadUserByUsername_deberiaBuscarUsandoEmailRecibido() {

        String email = "usuario@test.com";

        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.of(usuario));

        userDetailsService.loadUserByUsername(email);

        verify(usuarioRepository)
                .findByEmail(email);
    }

    // =========================================================
    // NULL
    // =========================================================

    @Test
    void loadUserByUsername_deberiaPermitirQueRepositoryManejeEmailNull() {

        when(usuarioRepository.findByEmail(null))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(null)
        );

        verify(usuarioRepository)
                .findByEmail(null);
    }

    // =========================================================
    // INTERACCIONES
    // =========================================================

    @Test
    void loadUserByUsername_deberiaConsultarRepositoryUnaSolaVez() {

        when(usuarioRepository.findByEmail("nahuel@gmail.com"))
                .thenReturn(Optional.of(usuario));

        userDetailsService.loadUserByUsername("nahuel@gmail.com");

        verify(usuarioRepository, times(1))
                .findByEmail("nahuel@gmail.com");

        verifyNoMoreInteractions(usuarioRepository);
    }

    @Test
    void loadUserByUsername_noDeberiaBuscarOtraVezSiUsuarioNoExiste() {

        when(usuarioRepository.findByEmail("noexiste@gmail.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(
                        "noexiste@gmail.com"
                )
        );

        verify(usuarioRepository, times(1))
                .findByEmail("noexiste@gmail.com");

        verifyNoMoreInteractions(usuarioRepository);
    }
}