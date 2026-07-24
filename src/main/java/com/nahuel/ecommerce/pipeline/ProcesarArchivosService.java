package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
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

    public ResultadoImportacionDto procesarArchivo(MultipartFile file) throws IOException {

        ResultadoImportacionDto resultado = new ResultadoImportacionDto();

        List<ProductoExcelDto> listProductoDto = excelProcessingService.leerExcel(file);
        List<ProductoExcelDto> listProductosFiltrados= limpiezaDatosService.limpiar(listProductoDto, resultado);
        List<ProductoExcelDto> listProductosNormalizados= normalizacionDatosService.normalizar(listProductosFiltrados);
        List<Producto> productosTransformados= transformacionDatosService.transformar(listProductosNormalizados,resultado);

        creacionEntidadesJpaProductos(productosTransformados,resultado);
        return resultado;
    }

    private void creacionEntidadesJpaProductos(List<Producto> productos,ResultadoImportacionDto resultado) {
        int importados=0;
        for (Producto pro : productos) {
            if (!productoRepository.existsByNombre(pro.getNombre())) {
                productoRepository.save(pro);
                importados++;
            } else {
                resultado.setLineasDescartadasPorDuplicados(
                        resultado.getLineasDescartadasPorDuplicados() + 1
                );
            }
        }
        resultado.setProductosImportados(importados);
    }

}
