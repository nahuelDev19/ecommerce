package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.services.ProductoServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoServiceImp productoService;

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody ProductoDto dto){
        ProductoDto ndto=productoService.guardarProducto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ndto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        productoService.descontinuarProducto(id);
        return ResponseEntity.noContent().build();
    }

}
