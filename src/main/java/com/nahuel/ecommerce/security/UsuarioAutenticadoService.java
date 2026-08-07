package com.nahuel.ecommerce.security;

import com.nahuel.ecommerce.entitys.Rol;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.exceptions.AccesoDenegadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;


    public Usuario actual() {
        String email= SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return usuarioRepository.findByEmail(email).orElseThrow(()-> new
                RecursoNoEncontradoException("El usuario no fue encontrado"));
    }

    /** Valida propiedad del recurso: dueño o ADMIN. */
    public void verificarPropietario(UUID idPropietario) {
       Usuario usuario= actual();

       if (usuario.getRol().name().equalsIgnoreCase("propietario")){
           return;
       }
       if (!usuario.getId().equals(idPropietario)) {
            throw new AccesoDenegadoException();
        }
    }

}
