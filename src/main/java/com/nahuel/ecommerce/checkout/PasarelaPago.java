package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.Orden;

public interface PasarelaPago {

    // Contrato de nuestra aplicación: el adapter traduce los datos del proveedor.
    PreferenciaPago crearPreferencia(Orden orden);

    PagoConsultado consultarPago(String pagoExternoId);
}
