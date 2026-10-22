package com.nahuel.ecommerce.dtos;

import java.util.UUID;

public record CheckoutResponseDto(UUID ordenId, String urlPago) {
}
