package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.Orden;

public interface PasarelaPago {

    String crearUrlPago(Orden orden);

}
