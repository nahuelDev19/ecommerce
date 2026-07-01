package com.nahuel.ecommerce.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaPaginaProductoDto {

    private List<ProductoDto> contenido;

    private Integer pagina;

    private Integer tamanio;

    private Long totalElementos;

    private Integer totalPaginas;

}
