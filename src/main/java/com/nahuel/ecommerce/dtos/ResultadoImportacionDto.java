    package com.nahuel.ecommerce.dtos;

    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @AllArgsConstructor @Data @NoArgsConstructor
    public class ResultadoImportacionDto {


        private int lineasProcesadas;
        private int lineasDescartadasPorCamposVacios;
        private int lineasDescartadasPorFormatoInvalido;
        private int lineasDescartadasPorDuplicados;
        private int productosImportados;


    }