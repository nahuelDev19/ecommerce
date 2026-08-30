package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.AccesoDenegadoException;
import com.nahuel.ecommerce.exceptions.NoAutenticadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.nahuel.ecommerce.entitys.Rol.ADMIN;
import static com.nahuel.ecommerce.entitys.Rol.PROPIETARIO;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;


    public Usuario actual() {

        Authentication auth= SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email= Optional.ofNullable(auth)
                .filter(a-> !(a instanceof AnonymousAuthenticationToken))
                .map(Authentication::getName)
                .orElseThrow(NoAutenticadoException::new);

        return usuarioRepository.findByEmail(email).orElseThrow(()-> new
                RecursoNoEncontradoException("El usuario no fue encontrado"));
    }

    /** Valida propiedad del recurso: dueño o ADMIN. */
    public void verificarPropietario(UUID idPropietario) {
       Usuario usuario= actual();

       if (usuario.getRol() == ADMIN) {
           return;
       }

       if (!usuario.getId().equals(idPropietario)) {
            throw new AccesoDenegadoException();
        }
    }

}
