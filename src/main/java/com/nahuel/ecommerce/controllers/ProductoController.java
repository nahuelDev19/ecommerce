package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.FiltrosBusquedaProductoDto;
import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.services.ImportacionService;
import com.nahuel.ecommerce.services.ProductoService;
import com.nahuel.ecommerce.services.ProductoServiceImp;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final ImportacionService importacionService;

    @PostMapping
    public ResponseEntity<?> crearProducto(@Valid @RequestBody ProductoDto dto){
        ProductoDto ndto=productoService.guardarProducto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ndto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        productoService.descontinuarProducto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/desactivos")
    public ResponseEntity<List<ProductoDto>> listarDesactivos() {
        return ResponseEntity.ok(productoService.listarDesactivos());
    }

    @GetMapping("/filtrados")
    public ResponseEntity<Page<ProductoDto>> buscarProductoFiltro(@Valid @ModelAttribute FiltrosBusquedaProductoDto dto) {
        return ResponseEntity.ok(productoService.buscarProductos(dto));
    }

    @DeleteMapping("/{id}/definitivo")
    public ResponseEntity<Void> eliminarDefinitivo(@PathVariable UUID id){
        productoService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/descontinuados")
    public ResponseEntity<List<ProductoDto>> listarDescontinuados() {
        return ResponseEntity.ok(productoService.listarDescontinuados());
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarProducto(@Valid @RequestBody ProductoDto dto,@PathVariable UUID id){
        return ResponseEntity.ok(productoService.actualizarPorId(id,dto));
    }

    @PostMapping("/upload")
    public ResponseEntity<UUID> upload(@RequestParam MultipartFile archivo,@RequestParam(defaultValue = "false") boolean soloValidacion) {
        UUID id = importacionService.subirArchivo(archivo, soloValidacion);
        return ResponseEntity.accepted().header("Location","/api/productos/importaciones/"+id).body(id);
    }
}
