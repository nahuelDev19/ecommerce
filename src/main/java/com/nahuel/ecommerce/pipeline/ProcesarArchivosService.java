package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.config.LimitesImportacion;
import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.dtos.ResultadoImportacionDto;
import com.nahuel.ecommerce.entitys.Producto;
import com.nahuel.ecommerce.exceptions.ArchivoConErroresException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.repositories.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.toSet;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProcesarArchivosService {

    private final ProcesarExcelService excelProcessingService;
    private final ProductoRepository productoRepository;
    private final LimpiezaDatosService limpiezaDatosService;
    private final NormalizacionDatosService normalizacionDatosService;
    private final TransformacionDatosService transformacionDatosService;
    private final LimitesImportacion limitesImportacion;
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public ResultadoImportacionDto procesarArchivo(Path file){


        ValidacionImportacion v= validarArchivo(file);
        ResultadoImportacionDto resultado= confirmar(v);

        return resultado;
    }


    public ValidacionImportacion validarArchivo(Path file){
        ResultadoImportacionDto reporte= new ResultadoImportacionDto();

        List<ProductoExcelDto> listProductoDto = excelProcessingService.leerExcel(file);
        List<ProductoExcelDto> listProductosFiltrados= limpiezaDatosService.limpiar(listProductoDto, reporte);
        List<ProductoExcelDto> listProductosNormalizados= normalizacionDatosService.normalizar(listProductosFiltrados);
        List<Producto> productosTransformados= transformacionDatosService.transformar(listProductosNormalizados,reporte);

Set<String> nombres = productosTransformados.stream().map(Producto::getNombre).collect(toSet());
        Set<String> existentes = nombres.isEmpty() ? Set.of() : new HashSet<>(productoRepository.findNombresExistentes(nombres));
        Set<String> vistos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<Producto> validos = productosTransformados.stream()
                .filter(p -> !existentes.contains(p.getNombre()))
                .filter(p -> vistos.add(p.getNombre()))
                .toList();
        reporte.setLineasDescartadasPorDuplicados(productosTransformados.size()-validos.size());
        reporte.setEstaLimpio(!new ValidacionImportacion(validos, reporte).tieneErrores());

        return new ValidacionImportacion(validos, reporte);

    }

    public ResultadoImportacionDto confirmar(ValidacionImportacion v) {
        if (v.tieneErrores()) throw new ArchivoConErroresException("Archivo con errores, no se guarda",v.reporte());

        List<Producto> aGuardar= v.productosValidos();
        int lote= limitesImportacion.getTamanioLote();
        int guardados = 0;

        for(int i=0; i< aGuardar.size() ;i+= lote){
            List<Producto> chunk= aGuardar.subList(i,Math.min(i+lote,aGuardar.size()));

            guardarLote(chunk);
            guardados+= chunk.size();
            log.info("importacion: {} espacio de {} productos guardados");


        }

        v.reporte().setProductosImportados(guardados);
        return v.reporte();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarLote(List<Producto> chunk){
        productoRepository.saveAll(chunk);
        entityManager.flush();
        entityManager.clear();
    }


}
