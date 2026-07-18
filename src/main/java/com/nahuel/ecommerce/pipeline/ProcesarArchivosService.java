package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProcesarArchivosService {

    private final ProcesarExcelService excelProcessingService;
    private final ProductoRepository productoRepository;


    public void procesarArchivo(MultipartFile file) throws IOException {

        List<ProductoExcelDto> listProductoDto = excelProcessingService.leerExcel(file);
        List<Producto> listProduct= listProductoDto.stream().map(productoExcelDto -> {
            return new Producto(
                    productoExcelDto.getNombre(),
                    productoExcelDto.getDescripcion(),
                    new BigDecimal(productoExcelDto.getPrecio()),
                    productoExcelDto.getMoneda(),
                    Boolean.parseBoolean(productoExcelDto.getActivo())
            );
        }).toList();

        creacionEntidadesJpaProductos(listProduct);
    }

    private void creacionEntidadesJpaProductos(List<Producto> productos) {
        for (Producto pro : productos) {
            Producto productosEntitys = new Producto(
                    pro.getNombre(),
                    pro.getDescripcion(),
                    pro.getPrecioBase(),
                    pro.getMoneda(),
                    pro.getActivo()
            );
            productosEntitys.setFechaCreacion(Instant.now());
            productoRepository.save(productosEntitys);
        }
    }

}
