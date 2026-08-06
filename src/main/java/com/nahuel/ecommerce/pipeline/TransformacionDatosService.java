package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ErrorImportacionDto;
import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.Producto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransformacionDatosService {


    public List<Producto> transformar(List<ProductoExcelDto> datos, ResultadoImportacionDto resultado) {

        List<Producto> lista = new ArrayList<>();

        for (ProductoExcelDto dto : datos) {

            try {

                validarDatosCorrectos(dto);

                Producto producto = new Producto(
                        dto.getNombre(),
                        dto.getDescripcion(),
                        new BigDecimal(dto.getPrecio()),
                        dto.getMoneda(),
                        Boolean.parseBoolean(dto.getActivo()),
                        Instant.now()
                );

                lista.add(producto);

            } catch (RuntimeException e) {
                resultado.setLineasDescartadasPorFormatoInvalido(
                        resultado.getLineasDescartadasPorFormatoInvalido() + 1);
                resultado.getErrores().add(  new ErrorImportacionDto(
                        dto.getNombre(),
                        e.getMessage()
                ) );
            }
        }

        return lista;
    }


    private void validarDatosCorrectos(ProductoExcelDto dto) {
        List<String> errores = new ArrayList<>();


        // Precio
        try {
            BigDecimal precio = new BigDecimal(dto.getPrecio());

            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                errores.add("El precio debe ser mayor a 0");
            }
        } catch (NumberFormatException e) {
            errores.add("El precio debe contener un valor numérico válido");
        }

        // Moneda
        if (dto.getMoneda() == null ||
                (!dto.getMoneda().equalsIgnoreCase("ARS") &&
                        !dto.getMoneda().equalsIgnoreCase("USD") &&
                        !dto.getMoneda().equalsIgnoreCase("EUR"))) {

            errores.add("La moneda debe ser ARS, USD o EUR");
        }

        // Activo
        if (dto.getActivo() == null ||
                (!dto.getActivo().equalsIgnoreCase("true") &&
                        !dto.getActivo().equalsIgnoreCase("false"))) {

            errores.add("El campo activo debe ser true o false");
        }

        if (!errores.isEmpty()) {
            throw new RuntimeException(String.join(", ", errores));
        }
    }



}
