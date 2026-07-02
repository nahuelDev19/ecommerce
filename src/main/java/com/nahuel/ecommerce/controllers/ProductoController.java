package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.services.ProductoServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoServiceImp productoService;


}
