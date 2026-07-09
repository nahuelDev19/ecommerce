package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.services.CarritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;

    @PostMapping
    public CarritoDto guardar(@RequestBody CarritoDto dto) {
        return carritoService.guardar(dto);
    }

    @GetMapping
    public List<CarritoDto> listar() {

        return carritoService.listar();
    }

    @GetMapping("/{id}")
    public CarritoDto buscar(@PathVariable UUID id) {

        return carritoService.buscarPorId(id)
                .orElseThrow();
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {

        carritoService.eliminar(id);
    }
}