package com.nahuel.ecommerce.controllers;

import com.nahuel.ecommerce.services.CuponeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CuponIdorTest {

    @Mock
    private CuponeService cuponeService;

    @InjectMocks
    private CuponController cuponController;

    private MockMvc mockMvc;

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(cuponController)
                .setValidator(validator)
                .build();
    }

    @Test
    void deberiaConsultarCuponPorId() throws Exception {
        UUID idCupon = UUID.randomUUID();

        when(cuponeService.buscarCuponId(idCupon))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/cupones/{id}", idCupon)
                )
                .andExpect(status().isOk());

        verify(cuponeService).buscarCuponId(idCupon);
    }

    @Test
    void deberiaEliminarCuponPorId() throws Exception {
        UUID idCupon = UUID.randomUUID();

        when(cuponeService.eliminarCuponPorId(idCupon))
                .thenReturn(true);

        mockMvc.perform(
                        delete("/api/cupones/{id}", idCupon)
                )
                .andExpect(status().isNoContent());

        verify(cuponeService).eliminarCuponPorId(idCupon);
    }

    @Test
    void deberiaConsultarCuponPorCodigo() throws Exception {
        String codigoCupon = "DESCUENTO10";

        when(cuponeService.buscarPorCodigo(codigoCupon))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/cupones/cupones/{codigo}", codigoCupon)
                )
                .andExpect(status().isOk());

        verify(cuponeService).buscarPorCodigo(codigoCupon);
    }

    @Test
    void noDeberiaCrearCuponConDatosInvalidos() throws Exception {
        String jsonInvalido = """
                {
                    "codigoCupon": "",
                    "descripcion": "",
                    "valorDescuento": 0,
                    "montoMinimoCarrito": -10,
                    "limiteUsoPorUsuario": 0
                }
                """;

        mockMvc.perform(
                        post("/api/cupones")
                                .contentType(APPLICATION_JSON)
                                .content(jsonInvalido)
                )
                .andExpect(status().isBadRequest());

        verify(cuponeService, never()).crearCupon(org.mockito.ArgumentMatchers.any());
    }
}
