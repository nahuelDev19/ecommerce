package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional
@Service
public class UsuarioServiceImp implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UsuarioDto guardar(UsuarioDto dto) {

        Usuario usuario = toEntity(dto);

        usuario = usuarioRepository.save(usuario);

        return toDto(usuario);
    }

    @Override
    public List<UsuarioDto> listar() {

        return usuarioRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Optional<UsuarioDto> buscarPorId(UUID id) {
        return Optional.of(
                usuarioRepository.findById(id)
                        .map(this::toDto)
                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
        );
    }

    @Override
    public void eliminar(UUID id) {
        Usuario usuario= usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("usuario no encotrado"));
        usuarioRepository.delete(usuario);
    }

    private UsuarioDto toDto(Usuario usuario) {

        UsuarioDto dto = new UsuarioDto();

        dto.setId(usuario.getId());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setEmail(usuario.getEmail());


        return dto;
    }

    private Usuario toEntity(UsuarioDto dto) {

        Usuario usuario = new Usuario();

        usuario.setId(dto.getId());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setContrasena(dto.getContrasena());
        usuario.setEmail(dto.getEmail());

        return usuario;
    }
}