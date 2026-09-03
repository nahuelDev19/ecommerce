package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtFilter = new JwtFilter(jwtService, userDetailsService, usuarioRepository);
    }

    @Test
    void sinAuthorizationHeader_dejaPasarSinUsarJwt() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, usuarioRepository);
        verify(response, never()).sendError(anyInt(), anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void headerNoBearer_dejaPasarSinUsarJwt() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Token abc");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService, userDetailsService, usuarioRepository);
        verify(response, never()).sendError(anyInt(), anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void jwtInvalido_oExpirado_alExtraerSubject_envia401_yNoContinua() throws ServletException, IOException {
        String jwt = "malformed.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        when(jwtService.extractSubject(jwt)).thenThrow(new JwtException("expired"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Token inválido o expirado"
        );
        verify(filterChain, never()).doFilter(any(), any());
        verifyNoInteractions(userDetailsService, usuarioRepository);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void extractSubjectDevuelveStringNoUUID_envia401_yNoContinua() throws ServletException, IOException {
        String jwt = "some.jwt";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);

        when(jwtService.extractSubject(jwt)).thenReturn("not-a-uuid");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Token inválido o expirado"
        );
        verify(filterChain, never()).doFilter(any(), any());
        verifyNoInteractions(userDetailsService, usuarioRepository);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void securityContextYaTieneAuthentication_noCargaUsuario_yDejaPasar() throws ServletException, IOException {
        String jwt = "valid.jwt.token";
        UUID id = UUID.randomUUID();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractSubject(jwt)).thenReturn(id.toString());

        var existingAuth = new UsernamePasswordAuthenticationToken(
                "any",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(usuarioRepository, userDetailsService);
        verify(response, never()).sendError(anyInt(), anyString());
        verify(filterChain, times(1)).doFilter(request, response);
        assertSame(existingAuth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void usuarioNoExiste_envia401UsuarioNoEncontrado_yNoContinua() throws ServletException, IOException {
        String jwt = "valid.jwt.token";
        UUID id = UUID.randomUUID();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractSubject(jwt)).thenReturn(id.toString());
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Usuario no encontrado o cuenta desactivada"
        );
        verify(filterChain, never()).doFilter(any(), any());
        verifyNoInteractions(userDetailsService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenInvalidoTokenNoValido_envia401TokenInvalido_yNoContinua() throws ServletException, IOException {
        String jwt = "valid.jwt.token";
        UUID id = UUID.randomUUID();

        Usuario usuario = mock(Usuario.class);
        //when(usuario.getId()).thenReturn(id);
        when(usuario.getEmail()).thenReturn("test@email.com");
        when(usuario.getActivo()).thenReturn(true);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractSubject(jwt)).thenReturn(id.toString());
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        UserDetails userDetails = new User(
                usuario.getEmail(),
                "pwd",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(userDetailsService.loadUserByUsername(usuario.getEmail())).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, usuario)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Token inválido"
        );
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValido_seteaAuthentication_yDejaPasar() throws ServletException, IOException {
        String jwt = "valid.jwt.token";
        UUID id = UUID.randomUUID();

        Usuario usuario = mock(Usuario.class);
        //when(usuario.getId()).thenReturn(id);
        when(usuario.getEmail()).thenReturn("test@email.com");
        when(usuario.getActivo()).thenReturn(true);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwt);
        when(jwtService.extractSubject(jwt)).thenReturn(id.toString());
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails userDetails = new User("test@email.com", "pwd", authorities);

        when(userDetailsService.loadUserByUsername(usuario.getEmail())).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, usuario)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(response, never()).sendError(anyInt(), anyString());
        verify(filterChain, times(1)).doFilter(request, response);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);
        assertEquals(userDetails, auth.getPrincipal());
        assertNull(auth.getCredentials());

        assertNotNull(auth.getAuthorities());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());

        assertNotNull(auth.getDetails());
        assertTrue(auth.getDetails() instanceof WebAuthenticationDetails);
    }
}
