package com.nahuel.ecommerce.support;

import com.nahuel.ecommerce.checkout.PagoConsultado;
import com.nahuel.ecommerce.checkout.PasarelaPago;
import com.nahuel.ecommerce.checkout.PreferenciaPago;
import com.nahuel.ecommerce.entitys.Orden;

import java.util.concurrent.atomic.AtomicInteger;

public class PasarelaPagoFake implements PasarelaPago {
    public static final String URL_PAGO = "https://pasarela.test.invalid/pagar";
    public static final String PREFERENCIA_ID = "preferencia-fake-checkout";

    private final AtomicInteger solicitudes = new AtomicInteger();

    @Override
    public PreferenciaPago crearPreferencia(Orden orden) {
        solicitudes.incrementAndGet();
        return new PreferenciaPago(PREFERENCIA_ID, URL_PAGO);
    }

    @Override
    public PagoConsultado consultarPago(String pagoExternoId) {
        throw new UnsupportedOperationException("Este fake solo se usa para iniciar checkout");
    }

    public int solicitudesRealizadas() {
        return solicitudes.get();
    }

    public void reiniciar() {
        solicitudes.set(0);
    }
}
