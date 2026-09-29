package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.exceptions.ArchivoConErroresException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
@Service
public class ProcesarArchivosService {

    private final ProcesarExcelService excelProcessingService;
    private final ProductoRepository productoRepository;
    private final LimpiezaDatosService limpiezaDatosService;
    private final NormalizacionDatosService normalizacionDatosService;
    private final TransformacionDatosService transformacionDatosService;

    @Transactional
    public ResultadoImportacionDto procesarArchivo(MultipartFile file){


        ValidacionImportacion v= validarArchivo(file);
        ResultadoImportacionDto resultado= confirmar(v);

        return resultado;
    }


    public ValidacionImportacion validarArchivo(MultipartFile file){
        ResultadoImportacionDto reporte= new ResultadoImportacionDto();

        List<ProductoExcelDto> listProductoDto = excelProcessingService.leerExcel(file);
        List<ProductoExcelDto> listProductosFiltrados= limpiezaDatosService.limpiar(listProductoDto, reporte);
        List<ProductoExcelDto> listProductosNormalizados= normalizacionDatosService.normalizar(listProductosFiltrados);
        List<Producto> productosTransformados= transformacionDatosService.transformar(listProductosNormalizados,reporte);

        Set<String> nombres = productosTransformados.stream().map(Producto::getNombre).collect(toSet());
        Set<String> existentes = nombres.isEmpty() ? Set.of() : new HashSet<>(productoRepository.findNombresExistentes(nombres));
        Set<String> vistos = new HashSet<>();
        List<Producto> validos = productosTransformados.stream()
                .filter(p -> !existentes.contains(p.getNombre()))
                .filter(p -> vistos.add(p.getNombre()))
                .toList();
        reporte.setLineasDescartadasPorDuplicados(productosTransformados.size()-validos.size());
        reporte.setEstaLimpio(!new ValidacionImportacion(validos, reporte).tieneErrores());

        return new ValidacionImportacion(validos, reporte);

    }
    @Transactional
    public ResultadoImportacionDto confirmar(ValidacionImportacion v) {
        if (v.tieneErrores()) throw new ArchivoConErroresException("Archivo con errores, no se guarda",v.reporte());
        List<Producto> guardados = productoRepository.saveAll(v.productosValidos());
        v.reporte().setProductosImportados(guardados.size());
        return v.reporte();
    }

}
