package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.services.CuponeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final CuponeService cuponeService;

    @PostMapping
    public ResponseEntity<CuponDto> crearCupon(@RequestBody CuponDto dto) {
        return ResponseEntity.ok(cuponeService.crearCupon(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuponDto> buscarCuponId(@PathVariable UUID id) {
        return ResponseEntity.ok(cuponeService.buscarCuponId(id));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<CuponDto>> listarCupones() {
        return ResponseEntity.ok(cuponeService.listarCupones());
    }

    @GetMapping("/listar/activos")
    public ResponseEntity<List<CuponDto>> listarCuponesPorEstadoActivo() {
        return ResponseEntity.ok(cuponeService.listarCuponesPorEstadoActivo());
    }

    @GetMapping("/listar/inactivos")
    public ResponseEntity<List<CuponDto>> listarCuponesPorEstadoDesactivo() {
        return ResponseEntity.ok(cuponeService.listarCuponesPorEstadoDesactivo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCuporPorId(@PathVariable UUID id) {
        cuponeService.eliminarCuponPorId(id);
        return ResponseEntity.noContent().build();
    }

}
