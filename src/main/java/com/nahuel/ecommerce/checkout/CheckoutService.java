package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.EstadoOrden;
import java.util.UUID;

public interface CheckoutService {

    String iniciarCheckout(UUID usuarioId);

    EstadoOrden consultarEstadoOrden(UUID ordenId, UUID usuarioId);



}
