package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.AgregarItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
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
    public ResponseEntity<?> agregarItemCarrito(@RequestBody AgregarItemRequestDto dto){
        CarritoDto carritoDto= itemCarritoService.agregarItem(dto);
        return ResponseEntity.ok(carritoDto);
    }


    /*@PostMapping("/{carritoId}/items")
    public AgregarItemRequestDto agregarItemsCarrito(@RequestBody AgregarItemRequestDto dto) {
        return itemCarritoService.agregarProducto(dto);
    }

     */

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