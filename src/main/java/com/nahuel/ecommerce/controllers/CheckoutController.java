package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.checkout.CheckoutService;
import com.nahuel.ecommerce.dtos.CheckoutResponseDto;
import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.entitys.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<CheckoutResponseDto> iniciarCheckout(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(checkoutService.iniciarCheckout(usuario.getId()));
    }

    @GetMapping("/ordenes/{ordenId}/estado")
    public ResponseEntity<EstadoOrden> consultarEstadoOrden(
            @PathVariable UUID ordenId,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(checkoutService.consultarEstadoOrden(ordenId, usuario.getId()));
    }
}
