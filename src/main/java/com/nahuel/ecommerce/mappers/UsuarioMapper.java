package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.entitys.OrigenRegistro;
import com.nahuel.ecommerce.entitys.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsuarioMapper {

    /** Respuesta de autenticación (TokenResponseDto.usuario). */
   /* public UsuarioDto toDto(Usuario usuario) {
        UsuarioDto dto = new UsuarioDto();

        dto.setId(usuario.getId());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setEmail(usuario.getEmail());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getActivo());
        dto.setAccesoLocal(usuario.getOrigenRegistro() == OrigenRegistro.LOCAL);
        dto.setAccesoGoogle(usuario.getOrigenRegistro() == OrigenRegistro.GOOGLE);

        return dto;
    }

    */

    /** Respuesta pública de /api/usuarios: no expone rol ni estado. */
    public UsuarioDto toResumenDto(Usuario usuario) {
        UsuarioDto dto = new UsuarioDto();
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setEmail(usuario.getEmail());
        return dto;
    }

    public List<UsuarioDto> toResumenDto(List<Usuario> usuarios) {
        return usuarios.stream().map(this::toResumenDto).toList();
    }

    public Usuario toEntity(UsuarioDto dto) {
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setEmail(dto.getEmail());
        return usuario;
    }
}