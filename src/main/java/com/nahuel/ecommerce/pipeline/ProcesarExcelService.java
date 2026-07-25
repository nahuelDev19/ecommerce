package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
@Service
public class ProcesarExcelService {


    public List<ProductoExcelDto> leerExcel(MultipartFile file) throws IOException {
        List<ProductoExcelDto> lista = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is);) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow= sheet.getRow(0);

            if (headerRow==null){
                throw new RuntimeException("El documento debe contener los Encabezados");
            }

            validarColumna(headerRow);

            boolean primeraFila= true;
            for (Row row : sheet) {

                if (primeraFila){
                    primeraFila= false;
                    continue;
                }

                ProductoExcelDto dto = new ProductoExcelDto();
                dto.setNombre(obtenerValorCelda(row.getCell(0)));
                dto.setDescripcion(obtenerValorCelda(row.getCell(1)));
                dto.setMoneda(obtenerValorCelda(row.getCell(2)));
                dto.setPrecio(obtenerValorCelda(row.getCell(3)));
                dto.setActivo(obtenerValorCelda(row.getCell(4)));
                lista.add(dto);
            }

        }
        catch (IOException e) {
            throw new RuntimeException("Error al procesar archivo Excel", e);
        }

        return lista;
    }


    private void validarColumna(Row row){

        List<String> columnaObligatoria= List.of("nombre","descripcion","moneda","precio","activo");
        List<String> columnasExistentes= new ArrayList<>();
        List<String> faltante= new ArrayList<>();
        for (Cell cel:row){
            columnasExistentes.add(cel.getStringCellValue().trim().toLowerCase());
        }

        for (String col: columnaObligatoria){
            if (!columnasExistentes.contains(col)){
                faltante.add(col);
            }
        }

        if (!faltante.isEmpty()){
            throw new RuntimeException("no pueden faltar columnas: "+ faltante);
        }
    }

    public static String obtenerValorCelda(Cell cell) {

        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {

            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate fecha = cell.getDateCellValue()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    return fecha.toString(); // formato ISO yyyy-MM-dd
                } else {
                    double valor = cell.getNumericCellValue();

                    // Evita que 1500.0 salga como "1500.0"
                    if (valor == (long) valor) {
                        return String.valueOf((long) valor);
                    }

                    return String.valueOf(valor);
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                throw new RuntimeException("No se permiten fórmulas en el archivo Excel.");
            case BLANK:
                return "";

            default:
                return "";
        }
    }



}
