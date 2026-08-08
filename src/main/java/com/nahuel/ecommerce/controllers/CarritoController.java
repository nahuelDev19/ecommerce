package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.dtos.ItemRequestDto;
import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.EliminarItemCarritoDto;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.services.CarritoService;
import com.nahuel.ecommerce.services.CuponeService;
import com.nahuel.ecommerce.services.ItemCarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;
    private final ItemCarritoService itemCarritoService;
    private final CuponeService cuponeService;

    @PostMapping
    public CarritoDto guardar(@Valid @RequestBody CarritoDto dto) {
        return carritoService.guardar(dto);
    }

    @PostMapping("/agregar")
    public ResponseEntity<?> agregarItemCarrito(@Valid @RequestBody ItemRequestDto dto,@AuthenticationPrincipal  Usuario usuario){
        UUID idUsuario= usuario.getId();
        CarritoDto carritoDto= itemCarritoService.agregarItem(dto,idUsuario);
        return ResponseEntity.ok(carritoDto);
    }

    @PostMapping("/disminuir")
    public ResponseEntity<?> disminuirItemCarrito(@Valid @RequestBody ItemRequestDto dto,@AuthenticationPrincipal  Usuario usuario){
        UUID idUsuario= usuario.getId();
        CarritoDto carritoDto= itemCarritoService.disminuirItem(dto,idUsuario);
        return ResponseEntity.ok(carritoDto);
    }

    @DeleteMapping("/eliminar/item")
    public ResponseEntity<?> eliminarItemCarrito(@Valid @RequestBody EliminarItemCarritoDto dto,@AuthenticationPrincipal  Usuario usuario){
        UUID idUsuario= usuario.getId();
        CarritoDto carritoDto= itemCarritoService.eliminarItem(dto,idUsuario);
        return ResponseEntity.ok(carritoDto);
    }



    @GetMapping
    public List<CarritoDto> listar() {
        return carritoService.listar();
    }

    @GetMapping("/{id}")
    public CarritoDto buscar(@PathVariable UUID id,@AuthenticationPrincipal Usuario usuario) {
        return carritoService.buscarPorId(id,usuario.getId());
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable UUID id) {

        carritoService.eliminar(id);
    }


    @PutMapping("/{idCarrito}/cupon/{codigo}")
    public ResponseEntity<CarritoDto> aplicarCupon(@PathVariable UUID idCarrito, @PathVariable String codigo,
                                                   @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(cuponeService.aplicarCupon(idCarrito,codigo,usuario.getId()));
    }
    @PutMapping("/quitar/{idCarrito}/")
    public ResponseEntity<CarritoDto> quitarCupon(@PathVariable UUID idCarrito) {
        return ResponseEntity.ok(cuponeService.quitarCupon(idCarrito));
    }
}