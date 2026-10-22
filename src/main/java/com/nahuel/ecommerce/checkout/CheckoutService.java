package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.dtos.CheckoutResponseDto;
import java.util.UUID;

public interface CheckoutService {

    CheckoutResponseDto iniciarCheckout(UUID usuarioId);

    EstadoOrden consultarEstadoOrden(UUID ordenId, UUID usuarioId);



}
