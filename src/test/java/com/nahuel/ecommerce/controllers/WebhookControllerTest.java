package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.checkout.WebhookService;
import com.nahuel.ecommerce.exceptions.FirmaWebhookInvalidaException;
import com.nahuel.ecommerce.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebhookControllerTest {
    private WebhookService servicio;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        servicio = mock(WebhookService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WebhookController(servicio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void avisoCompletoSeDelegaYDevuelve200() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .param("data.id", "123")
                        .header("x-signature", "firma-de-prueba")
                        .header("x-request-id", "request-test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isOk());

        verify(servicio).process("123", "payment", "firma-de-prueba", "request-test");
    }

    @Test
    void sinSignatureLlegaAlServicioYConservaEl401DeFirmaInvalida() throws Exception {
        doThrow(new FirmaWebhookInvalidaException()).when(servicio)
                .process("123", "payment", null, "request-test");

        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .param("data.id", "123").header("x-request-id", "request-test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("INVALID_WEBHOOK_SIGNATURE"));

        verify(servicio).process("123", "payment", null, "request-test");
    }

    @Test
    void sinRequestIdSeDelegaComoNullSin400Automatico() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .param("data.id", "123").header("x-signature", "firma-de-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isOk());

        verify(servicio).process("123", "payment", "firma-de-prueba", null);
    }

    @Test
    void sinDataIdSeDelegaComoNullSin400Automatico() throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .header("x-signature", "firma-de-prueba").header("x-request-id", "request-test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isOk());

        verify(servicio).process(null, "payment", "firma-de-prueba", "request-test");
    }

    @Test
    void sinParametrosNoOcultaLaFirmaInvalida() throws Exception {
        doThrow(new FirmaWebhookInvalidaException()).when(servicio)
                .process(null, "payment", null, null);

        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isUnauthorized());

        verify(servicio).process(null, "payment", null, null);
    }

    @Test
    void falloInternoSeConfirmaCon200() throws Exception {
        doThrow(new IllegalStateException("Fallo interno simulado")).when(servicio)
                .process("123", "payment", "firma-de-prueba", "request-test");

        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .param("data.id", "123").header("x-signature", "firma-de-prueba")
                        .header("x-request-id", "request-test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isOk());

        verify(servicio).process("123", "payment", "firma-de-prueba", "request-test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"uq_pago_externo_id", "fk_pago_orden"})
    void falloDeIntegridadPropagadoPorServicioDevuelve200(String restriccion) throws Exception {
        var causa = new ConstraintViolationException("Restricción simulada",
                new SQLException("Fallo SQL de prueba"), restriccion);
        doThrow(new DataIntegrityViolationException("Fallo de integridad simulado", causa))
                .when(servicio).process("123", "payment", "firma-de-prueba", "request-test");

        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .param("data.id", "123").header("x-signature", "firma-de-prueba")
                        .header("x-request-id", "request-test")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"payment\"}"))
                .andExpect(status().isOk());

        verify(servicio).process("123", "payment", "firma-de-prueba", "request-test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{"})
    void cuerpoAusenteOMalFormadoConserva400(String cuerpo) throws Exception {
        mockMvc.perform(post("/api/webhooks/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }
}
