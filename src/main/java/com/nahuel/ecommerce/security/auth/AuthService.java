package com.nahuel.ecommerce.security.auth;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.RefreshTokenRepository;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AuthService {

    // registrar
    // login

        // intercambiar token google, (validar email null, validar si ya esta registrado con google (supabaseid con contenido) vincular )

    // armar respuesta
    // verificar activa
    // normalizar
    // nombre desde token
    //


    //--------------------------

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SupabaseTokenVerifier supabaseTokenVerifier;
    //private final RefreshTokenRepository refreshTokenRepository;
    //private final RefreshTokenService refreshTokenService;


    public TokenResponseDto loginOAuth(String tokenSupabase) {

        // 1. Validar el JWT de Supabase
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

        Map<String, Object> appMetadata = jwt.getClaim("app_metadata");
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        Usuario usuario;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();
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

        return construirRespuesta(usuario);
    }

    public TokenResponseDto registrar(RegistroRequestDto request){
        Usuario usuario= Usuario.builder()
                .email(normalizarEmail(request.getEmail()))
                .nombreUsuario(request.getNombreUsuario())
                .activo(true)
                .rol(Rol.CLIENTE)
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .build();

        Optional<Usuario> usuarioOpt=usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioOpt.isPresent()){
            throw new RuntimeException("el usuario ya se encuenta registrado");
        }
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
            //throw new UsuarioInactivoException(...);
            throw new RuntimeException("usuario no disponible");
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

        return dto;
    }


}
