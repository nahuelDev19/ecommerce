package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.services.CarritoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @PostMapping("/{usuarioId}")
    public Carrito guardar(@PathVariable UUID usuarioId) {
        return carritoService.guardar(usuarioId);
    }

    @GetMapping
    public List<Carrito> listar() {
        return carritoService.listar();
    }

    @GetMapping("/{id}")
    public Carrito buscar(@PathVariable UUID id) {
        return carritoService.buscarPorId(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {
        carritoService.eliminar(id);
    }

}
