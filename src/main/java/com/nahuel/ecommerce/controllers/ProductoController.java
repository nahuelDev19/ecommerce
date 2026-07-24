package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.FiltrosBusquedaProductoDto;
import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.pipeline.ProcesarArchivosService;
import com.nahuel.ecommerce.services.ProductoServiceImp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoServiceImp productoService;
    private final ProcesarArchivosService procesarArchivosService;

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

    @GetMapping("/desactivos")
    public ResponseEntity<List<ProductoDto>> listarDesactivos() {
        return ResponseEntity.ok(productoService.listarDesactivos());
    }

    @GetMapping("/filtrados")
    public ResponseEntity<Page<ProductoDto>> buscarProductoFiltro(@ModelAttribute FiltrosBusquedaProductoDto dto) {
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

    @PostMapping(value="/importar", consumes = "multipart/form-data")
    public ResponseEntity<List<ProductoDto>> importarDesdeExcel(@RequestParam("archivo") MultipartFile file) throws IOException {
        List<ProductoDto> listaCreados= productoService.cargarProductosDesdeExcel(file.getInputStream());
        return ResponseEntity.status(HttpStatus.CREATED).body(listaCreados);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable UUID id, ProductoDto dto){
        return ResponseEntity.ok(productoService.actualizarPorId(id,dto));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam MultipartFile archivo) throws IOException {
        //productoService.importar(archivo);
        ResultadoImportacionDto resultado=  procesarArchivosService.procesarArchivo(archivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
