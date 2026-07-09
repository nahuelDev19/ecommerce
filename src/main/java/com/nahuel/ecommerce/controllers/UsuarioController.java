package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.UsuarioDto;
import com.nahuel.ecommerce.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public UsuarioDto guardar(@RequestBody UsuarioDto dto) {

        return usuarioService.guardar(dto);
    }

    @GetMapping
    public List<UsuarioDto> listar() {

        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioDto buscar(@PathVariable UUID id) {

        return usuarioService.buscarPorId(id)
                .orElseThrow();
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {

        usuarioService.eliminar(id);
    }
}