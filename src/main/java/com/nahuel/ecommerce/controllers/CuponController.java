package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.dtos.CuponDtoResponse;
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
    public ResponseEntity<CuponDtoResponse> crearCupon(@RequestBody CuponDto dto) {
        return ResponseEntity.ok(cuponeService.crearCupon(dto));
    }

    @GetMapping("/listar")
    public ResponseEntity<List<CuponDtoResponse>> listarCupones() {
        return ResponseEntity.ok(cuponeService.listarCupones());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuponDtoResponse> buscarCuponId(@PathVariable UUID id) {
        return ResponseEntity.ok(cuponeService.buscarCuponId(id));
    }


    @GetMapping("/listar/activos")
    public ResponseEntity<List<CuponDtoResponse>> listarCuponesPorEstadoActivo() {
        return ResponseEntity.ok(cuponeService.listarCuponesPorEstadoActivo());
    }

    @GetMapping("/listar/inactivos")
    public ResponseEntity<List<CuponDtoResponse>> listarCuponesPorEstadoDesactivo() {
        return ResponseEntity.ok(cuponeService.listarCuponesPorEstadoDesactivo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCuporPorId(@PathVariable UUID id) {
        cuponeService.eliminarCuponPorId(id);
        return ResponseEntity.noContent().build();
    }

    //----------------------------------------
    @GetMapping("/{codigo}")
    public ResponseEntity<CuponDtoResponse> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(cuponeService.buscarPorCodigo(codigo));
    }




















}
