package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NormalizacionDatosService {


    public List<ProductoExcelDto> normalizar(List<ProductoExcelDto> datos) {
        for (ProductoExcelDto dto: datos ) {

            dto.setNombre(dto.getNombre().trim());
            dto.setDescripcion(dto.getDescripcion().trim());
            dto.setMoneda(dto.getMoneda().toUpperCase().trim());
            dto.setActivo(dto.getActivo().toUpperCase().trim());

            if(dto.getPrecio().isEmpty()){
                dto.setPrecio("0");
            }
            dto.setPrecio(normalizarPrecio(dto.getPrecio()));
        }
        return  datos;
    }
    private String normalizarPrecio(String precio) {
        return precio.trim()
                .replaceAll("[^0-9,.]", "")
                .replace(".", "")
                .replace(",", ".");
    }

}
