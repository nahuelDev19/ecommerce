package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.exceptions.AccesoDenegadoException;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.mappers.UsuarioMapper;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional
@Service
public class UsuarioServiceImp implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    @Override
    public UsuarioDto guardar(UsuarioDto dto) {

        Usuario usuario = usuarioMapper.toEntity(dto);

        usuario = usuarioRepository.save(usuario);

        return usuarioMapper.toResumenDto(usuario);
    }

    @Override
    public List<UsuarioDto> listar() {

        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResumenDto)
                .toList();
    }

    @Override
    public UsuarioDto buscarPorId(UUID id, UUID autenticado) {
        if (!id.equals(autenticado)) {
            throw new AccesoDenegadoException();
        }

        return usuarioRepository.findById(id)
                .map(usuarioMapper::toResumenDto)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException("Usuario no encontrado")
                );
    }

    @Override
    public void eliminar(UUID id,UUID autenticadoId) {

        if (!id.equals(autenticadoId)) {
            throw new AccesoDenegadoException();
        }

        Usuario usuario= usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("usuario no encotrado"));
        usuarioRepository.delete(usuario);
    }




}