package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public UsuarioDto guardar(@Valid @RequestBody UsuarioDto dto) {
        return usuarioService.guardar(dto);
    }

    @GetMapping
    public List<UsuarioDto> listar() {

        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioDto buscar(@PathVariable UUID id,@AuthenticationPrincipal Usuario autenticado) {
        return usuarioService.buscarPorId(id,autenticado.getId());

    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id,@AuthenticationPrincipal Usuario autenticado) {
        usuarioService.eliminar(id,autenticado.getId());
    }
}