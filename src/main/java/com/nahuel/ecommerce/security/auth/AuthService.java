package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.UsuarioInactivoException;
import com.nahuel.ecommerce.exceptions.UsuarioYaRegistradoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import com.nahuel.ecommerce.security.JwtService;
import com.nahuel.ecommerce.security.SupabaseTokenVerifier;
import com.nahuel.ecommerce.security.auth.dto.LoginRequestDto;
import com.nahuel.ecommerce.security.auth.dto.RegistroRequestDto;
import com.nahuel.ecommerce.security.auth.dto.TokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {



    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SupabaseTokenVerifier supabaseTokenVerifier;


    public TokenResponseDto loginOAuth(String tokenSupabase) {

        Jwt jwt = supabaseTokenVerifier.verificar(tokenSupabase);

        String emailCrudo = jwt.getClaimAsString("email");
        if (emailCrudo == null || emailCrudo.isBlank()) {
            throw new BadCredentialsException("el token de supabase no contiene email");
        }
        String email = normalizarEmail(emailCrudo);
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new BadCredentialsException("El token de Supabase no contiene subject");
        }

        UUID supabaseId ;
        try {
        supabaseId= UUID.fromString(jwt.getSubject());
        }catch (IllegalArgumentException e) {
            throw new BadCredentialsException("El subject de Supabase no es un UUID válido");
        }

        // Primero por supabaseId (identidad verificada por Supabase); el email
        // solo se consulta si no hay match, para no gastar una query al pedo.
        Optional<Usuario> porSupabaseId = usuarioRepository.findBySupabaseId(supabaseId);

        Usuario usuario;

        if (porSupabaseId.isPresent()) {
            usuario = porSupabaseId.get();
        } else {
            Optional<Usuario> porEmail = usuarioRepository.findByEmail(email);

            if (porEmail.isPresent()) {
                usuario = porEmail.get();

                // SEGURIDAD: una cuenta LOCAL (creada con contraseña) NUNCA se enlaza
                // automáticamente con OAuth. Si se hiciera, cualquiera podría registrar
                // localmente un email ajeno y, cuando el dueño entrara con Google, la
                // cuenta quedaría compartida con el atacante (que conserva la contraseña).
                if (usuario.getOrigenRegistro() != OrigenRegistro.GOOGLE) {
                    throw new BadCredentialsException(
                            "Ya existe una cuenta con este email creada con contraseña. " +
                            "Iniciá sesión con tu contraseña para vincularla.");
                }

                // Cuenta GOOGLE legada sin supabaseId: se completa la vinculación.
                if (usuario.getSupabaseId() == null) {
                    usuario.setSupabaseId(supabaseId);
                    usuario = usuarioRepository.save(usuario);
                }
            } else {
                usuario = Usuario.builder()
                        .email(email)
                        .origenRegistro(OrigenRegistro.GOOGLE)
                        .supabaseId(supabaseId)
                        .rol(Rol.CLIENTE)
                        .activo(true)
                        .build();

                usuario = usuarioRepository.save(usuario);
            }
        }

        // Un usuario desactivado no puede volver a entrar, ni siquiera por OAuth.
        verificarUsuarioActivo(usuario);

        return construirRespuesta(usuario);
    }

    public TokenResponseDto registrar(RegistroRequestDto request){
        String emailNormalizado= normalizarEmail(request.getEmail());
        if(usuarioRepository.findByEmail(emailNormalizado).isPresent()){
            throw new UsuarioYaRegistradoException();
        }
        Usuario usuario= Usuario.builder()
                .email(emailNormalizado)
                .nombreUsuario(request.getNombreUsuario())
                .activo(true)
                .rol(Rol.CLIENTE)
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .build();
        usuario.setOrigenRegistro(OrigenRegistro.LOCAL);
        Usuario usuarioGuardado= usuarioRepository.save(usuario);

        return construirRespuesta(usuarioGuardado);
    }

    public TokenResponseDto loguear(LoginRequestDto request){
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getContrasena()
                    )
            );
        }catch (BadCredentialsException e){
            throw new BadCredentialsException("username o password incorrectos");
        }
        Usuario usuario= usuarioRepository.findByEmail(normalizarEmail(request.getEmail())).orElseThrow();
        verificarUsuarioActivo(usuario);
        return construirRespuesta(usuario);

    }



    @Transactional
    public void logout() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if(auth== null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken || "anonymousUser".equals(auth.getName())){
            throw new BadCredentialsException("logout requiere access token valido en authorization");
        }

    }

    private TokenResponseDto construirRespuesta(Usuario usuario) {
        return TokenResponseDto.builder()
                .token(jwtService.generateToken(usuario))
                .tipo("Bearer")
                .expiraEnSegundos(jwtService.getExpirationInSeconds())
                .usuario(toDto(usuario))
                .build();
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }


    private void verificarUsuarioActivo(Usuario usuario) {
        if (!usuario.getActivo()) {
            throw new UsuarioInactivoException();
        }
    }

    private String obtenerIdDesdeToken(String token) {
        return jwtService.extractSubject(token);
    }

    private UsuarioDto toDto(Usuario usuario) {

        UsuarioDto dto = new UsuarioDto();

        dto.setId(usuario.getId());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setEmail(usuario.getEmail());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getActivo());
        if(usuario.getOrigenRegistro().equals(OrigenRegistro.LOCAL)){
        dto.setAccesoLocal(true);
        }else {
            dto.setAccesoLocal(false);
        }

        //carregir despues
        dto.setCreadoEn(Instant.now());

        return dto;
    }


}
