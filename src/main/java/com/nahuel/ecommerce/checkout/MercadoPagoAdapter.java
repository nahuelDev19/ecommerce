package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.dtos.PreferenciaMercadoPagoResponse;
import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.entitys.Orden;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MercadoPagoAdapter implements PasarelaPago {

    private final RestClient.Builder restClientBuilder;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.api-url}")
    private String apiURL;

    @Value("${mercadopago.notification-url}")
    private String notificationURL;

    @Override
    public String crearUrlPago(Orden orden) {
        if (orden == null || orden.getId() == null) {
            throw new IllegalStateException(
                    "La orden debe tener un identificador antes de solicitar el pago"
            );
        }
        if (orden.getEstado() != EstadoOrden.PENDIENTE_PAGO) {
            throw new ReglaNegocioException(
                    "Solo se puede solicitar el pago de una orden pendiente"
            );
        }
        if (orden.getTotal() == null || orden.getTotal().signum() <= 0) {
            throw new ReglaNegocioException("El total de la orden debe ser mayor que cero");
        }
        if (orden.getMoneda() == null) {
            throw new ReglaNegocioException("La orden no tiene una moneda definida");
        }
        if (orden.getCreadoEn() == null || orden.getVenceEn() == null) {
            throw new IllegalStateException(
                    "La orden debe tener fecha de creación y vencimiento"
            );
        }
        if (!Instant.now().isBefore(orden.getVenceEn())) {
            throw new ReglaNegocioException("La orden ya está vencida");
        }

        // Adaptamos el código interno sin modificar TipoMoneda ni sus datos.
        String moneda = switch (orden.getMoneda()) {
            case ARG -> "ARS";
            case USD -> "USD";
            case EUR -> "EUR";
        };

        // Un único concepto cobra el total final, incluyendo los descuentos.
        // El detalle de productos y precios permanece en los ItemOrden.
        Map<String, Object> item = Map.of(
                "id", orden.getId().toString(),
                "title", "Compra " + orden.getId(),
                "quantity", 1,
                "currency_id", moneda,
                "unit_price", orden.getTotal()
        );

        String inicio = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                orden.getCreadoEn().atOffset(ZoneOffset.UTC)
        );
        String vencimiento = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                orden.getVenceEn().atOffset(ZoneOffset.UTC)
        );

        // La referencia permite que el webhook encuentre nuestra orden.
        // Usamos el mismo vencimiento que guardamos en la base de datos.
        Map<String, Object> solicitud = Map.of(
                "items", List.of(item),
                "external_reference", orden.getId().toString(),
                "notification_url", notificationURL,
                "expires", true,
                "expiration_date_from", inicio,
                "expiration_date_to", vencimiento
        );

        // RestClient convierte el mapa a JSON y la respuesta al DTO.
        // Un error HTTP se propaga: no devolvemos una URL ficticia.
        PreferenciaMercadoPagoResponse respuesta = restClientBuilder.build()
                .post()
                .uri(apiURL + "/checkout/preferences")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(PreferenciaMercadoPagoResponse.class);

        if (respuesta == null || respuesta.getUrlPago() == null
                || respuesta.getUrlPago().isBlank()) {
            throw new IllegalStateException("Mercado Pago no devolvió una URL de pago");
        }
        return respuesta.getUrlPago();
    }
}
