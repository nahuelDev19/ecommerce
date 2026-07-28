package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserDetailsServiceImp implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Usuario usuario= usuarioRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("email del usuario no encontrado "+ email));
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getContrasena())
                .authorities("ROLE_"+ usuario.getRol().name())
                .build();
    }
}
