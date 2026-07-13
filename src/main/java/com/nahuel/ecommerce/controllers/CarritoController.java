package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;
import com.nahuel.ecommerce.services.CarritoService;
import com.nahuel.ecommerce.services.ItemCarritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;
    private final ItemCarritoService itemCarritoService;

    @PostMapping
    public CarritoDto guardar(@RequestBody CarritoDto dto) {
        return carritoService.guardar(dto);
    }

    @PostMapping("/agregar")
    public ResponseEntity<?> agregarItemCarrito(@RequestBody ItemRequestDto dto){
        CarritoDto carritoDto= itemCarritoService.agregarItem(dto);
        return ResponseEntity.ok(carritoDto);
    }

    @PostMapping("/disminuir")
    public ResponseEntity<?> disminuirItemCarrito(@RequestBody ItemRequestDto dto){
        CarritoDto carritoDto= itemCarritoService.disminuirItem(dto);
        return ResponseEntity.ok(carritoDto);
    }

    @DeleteMapping("/eliminar/item")
    public ResponseEntity<?> eliminarItemCarrito(@RequestBody EliminarItemCarritoDto dto){
        CarritoDto carritoDto= itemCarritoService.eliminarItem(dto);
        return ResponseEntity.ok(carritoDto);
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