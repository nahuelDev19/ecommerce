package com.nahuel.ecommerce.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private Long id;

    @JsonProperty("date_approved")
    private OffsetDateTime fechaAprobacion;

    @JsonProperty("status")
    private String estado;

    @JsonProperty("external_reference")
    private String referenciaExterna;

    @JsonProperty("transaction_amount")
    private BigDecimal monto;

    @JsonProperty("currency_id")
    private String moneda;
}
