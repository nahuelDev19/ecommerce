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
    private final LimpiezaDatosService limpiezaDatosService;
    private final NormalizacionDatosService normalizacionDatosService;
    private final TransformacionDatosService transformacionDatosService;

    public void procesarArchivo(MultipartFile file) throws IOException {

        List<ProductoExcelDto> listProductoDto = excelProcessingService.leerExcel(file);
        List<ProductoExcelDto> listProductosFiltrados= limpiezaDatosService.limpiar(listProductoDto);
        List<ProductoExcelDto> listProductosNormalizados= normalizacionDatosService.normalizar(listProductosFiltrados);
        List<Producto> productosTransformados= transformacionDatosService.transformar(listProductosNormalizados);

        creacionEntidadesJpaProductos(productosTransformados);
    }

    private void creacionEntidadesJpaProductos(List<Producto> productos) {
            productoRepository.saveAll(productos);
    }

}
