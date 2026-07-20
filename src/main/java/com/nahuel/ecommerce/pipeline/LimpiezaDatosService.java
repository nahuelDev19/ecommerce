package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;



@Service
public class LimpiezaDatosService {

    public List<ProductoExcelDto> limpiar(List<ProductoExcelDto> dto){
        return  dto.stream()
                .filter(data -> data.getNombre() != null && !data.getNombre().isBlank())

                .filter(data -> data.getPrecio() != null  &&  !data.getPrecio().isBlank())

                .filter(data -> data.getMoneda() != null  && !data.getMoneda().isBlank())

                .filter(data -> data.getDescripcion() != null && !data.getDescripcion().isBlank())

                .filter(data -> data.getActivo() != null && !data.getActivo().isBlank())

                .collect(Collectors.toList());

    }
}
