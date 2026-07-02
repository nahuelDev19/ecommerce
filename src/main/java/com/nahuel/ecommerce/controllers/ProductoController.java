package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.services.ProductoServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
