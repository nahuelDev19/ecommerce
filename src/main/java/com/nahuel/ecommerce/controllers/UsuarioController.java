package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.services.UsuarioService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@RestController
@RequestMapping("api/usuarios")
public class UsuarioController  {

    private final UsuarioService usuarioService;



    @PostMapping
    public Usuario guardar() {
        return usuarioService.guardar(new Usuario());
    }

    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public Usuario buscar(@PathVariable UUID id) {
        return usuarioService.buscarPorId(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        usuarioService.eliminar(id);
    }
}
