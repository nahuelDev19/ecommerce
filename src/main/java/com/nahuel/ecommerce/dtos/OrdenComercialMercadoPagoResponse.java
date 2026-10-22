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
public class OrdenComercialMercadoPagoResponse {
    private Long id;

    @JsonProperty("preference_id")
    private String preferenciaId;

    @JsonProperty("external_reference")
    private String referenciaExterna;
}
