package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NormalizacionDatosService {


    public List<ProductoExcelDto> normalizar(List<ProductoExcelDto> datos) {
        for (ProductoExcelDto dto: datos ) {

            dto.setNombre(dto.getNombre().trim());
            dto.setDescripcion(dto.getDescripcion().toLowerCase().trim());
            dto.setMoneda(dto.getMoneda().toUpperCase().trim());
            dto.setActivo(dto.getActivo().toUpperCase().trim());

            if(dto.getPrecio().isEmpty()){
                dto.setPrecio("0");
            }
            dto.setPrecio(dto.getPrecio().replace(",",".").replaceAll("[^0-9.]", "").trim());
        }
        return  datos;
    }


}
