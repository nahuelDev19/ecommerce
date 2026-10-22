package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.dtos.CheckoutResponseDto;
import com.nahuel.ecommerce.dtos.OrdenDto;
import java.util.List;
import java.util.UUID;

public interface CheckoutService {

    CheckoutResponseDto iniciarCheckout(UUID usuarioId);

    EstadoOrden consultarEstadoOrden(UUID ordenId, UUID usuarioId);

    OrdenDto consultarOrden(UUID ordenId, UUID usuarioId);

    List<OrdenDto> listarOrdenes(UUID usuarioId);

}
