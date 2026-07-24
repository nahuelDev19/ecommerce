package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoDto;
import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;



@Service
public class LimpiezaDatosService {
    public List<ProductoExcelDto> limpiar(List<ProductoExcelDto> productos,
                                          ResultadoImportacionDto resultado) {

        List<ProductoExcelDto> productosLimpios = productos.stream()
                .filter(this::esValido)
                .toList();

        resultado.setLineasProcesadas(productos.size());
        resultado.setLineasDescartadasPorCamposVacios(productos.size() - productosLimpios.size());

        return productosLimpios;
    }

    private boolean esValido(ProductoExcelDto data) {
        return data.getNombre() != null && !data.getNombre().isBlank()
                && data.getPrecio() != null && !data.getPrecio().isBlank()
                && data.getMoneda() != null && !data.getMoneda().isBlank()
                && data.getDescripcion() != null && !data.getDescripcion().isBlank()
                && data.getActivo() != null && !data.getActivo().isBlank();
    }

}
