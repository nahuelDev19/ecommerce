package com.nahuel.ecommerce.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreferenciaMercadoPagoResponse {

    // La preferencia habilita el checkout; esta URL todavía no acredita un pago.
    @JsonProperty("init_point")
    private String urlPago;
    // Identifica la preferencia, no un pago aprobado.
    @JsonProperty("id")
    private String preferenciaId;

}
