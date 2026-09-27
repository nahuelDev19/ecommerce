package com.nahuel.ecommerce.pipeline;

import com.nahuel.ecommerce.config.LimitesImportacion;
import com.nahuel.ecommerce.dtos.ProductoExcelDto;
import com.nahuel.ecommerce.exceptions.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
@RequiredArgsConstructor
@Service
public class ProcesarExcelService {

    private final LimitesImportacion limitesImportacion;


    public List<ProductoExcelDto> leerExcel(MultipartFile file) {


        int maxLargoCelda = limitesImportacion.getMaxLargoCelda();

        if(file.getSize() > limitesImportacion.getMaxTamanioBytes().toBytes()){
            throw new ArchivoDemasiadoGrandeException();
        }

        List<ProductoExcelDto> lista = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is);) {
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() >= limitesImportacion.getMaxFilas()){
                throw new DemasiadasFilasException();
            }

            Row headerRow= sheet.getRow(0);

            if (headerRow==null){
                throw new EncabezadoFaltanteException();
            }

            validarColumna(headerRow,maxLargoCelda);

            boolean primeraFila= true;
            for (Row row : sheet) {

                if (row.getLastCellNum() >limitesImportacion.getMaxColumnas()){
                    throw new DemasiadasColumnasException();
                }

                if (primeraFila){
                    primeraFila= false;
                    continue;
                }

                ProductoExcelDto dto = new ProductoExcelDto();
                dto.setNombre(obtenerValorCelda(row.getCell(0),maxLargoCelda));
                dto.setDescripcion(obtenerValorCelda(row.getCell(1),maxLargoCelda));
                dto.setMoneda(obtenerValorCelda(row.getCell(2),maxLargoCelda));
                dto.setPrecio(obtenerValorCelda(row.getCell(3),maxLargoCelda));
                dto.setActivo(obtenerValorCelda(row.getCell(4),maxLargoCelda));
                lista.add(dto);
            }

        }
        // Orden de catch: AppException PRIMERO (antes de IOException y de Exception).
        // Es el orden correcto y hay que mantenerlo: si este catch se moviera debajo de
        // catch (Exception), todos los errores de negocio quedarian enmascarados.
        //
        // Caso concreto: planilla valida (Excel legible) a la que le falta la columna "precio".
        // validarColumna() se ejecuta dentro del try y lanza ColumnasFaltantesException,
        // que es ReglaNegocioException -> AppException (BAD_REQUEST / BUSINESS_RULE).
        // Este primer catch la reenvia intacta, asi que GlobalExceptionHandler la mapea a un
        // ProblemDetail 400 con:
        //   status = 400
        //   title  = "BUSINESS_RULE"
        //   detail = "Faltan columnas obligatorias en el archivo: [precio]"
        // O sea: el usuario SI recibe el mensaje especifico de columnas faltantes, y NO el
        // generico "El archivo no tiene un formato Excel valido o esta danado" (ese es el
        // ultimo catch, reservado para Exception que no sea AppException).
        catch (AppException e){
            throw e;
        }

        catch (IOException e) {
            if (esZipBomb(e)) {
                throw new ArchivoComprimidoPeligrosoException(e);
            }

            throw new ErrorProcesamientoArchivoException(
                    "Error al procesar archivo Excel",
                    e
            );
        }

        catch (Exception e){
            throw new ErrorProcesamientoArchivoException("El archivo no tiene un formato Excel válido o está dañado", e);
        }

        return lista;
    }


    private void validarColumna(Row row, int maxLargoCelda){

        List<String> columnaObligatoria= List.of("nombre","descripcion","moneda","precio","activo");
        List<String> columnasExistentes= new ArrayList<>();
        List<String> faltante= new ArrayList<>();
        for (Cell cel:row){
            columnasExistentes.add(
                    obtenerValorCelda(cel,maxLargoCelda).toLowerCase()
            );
        }

        for (String col: columnaObligatoria){
            if (!columnasExistentes.contains(col)){
                faltante.add(col);
            }
        }

        if (!faltante.isEmpty()){
            throw new ColumnasFaltantesException(faltante);
        }
    }

    public String obtenerValorCelda(Cell cell, int maxLargoCelda) {

        if (cell == null) {
            return "";
        }

        String valor;

        switch (cell.getCellType()) {

            case STRING:
                valor= cell.getStringCellValue().trim();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate fecha = cell.getDateCellValue()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    valor= fecha.toString(); // formato ISO yyyy-MM-dd
                } else {
                    double numero = cell.getNumericCellValue();

                    if (numero == (long) numero) {
                        valor = String.valueOf((long) numero);
                    } else {
                        valor = String.valueOf(numero);
                    }
                }
                break;

            case BOOLEAN:
                valor =String.valueOf(cell.getBooleanCellValue());
                break;

            case FORMULA:
                throw new FormulaNoPermitidaException();
            case BLANK:
                valor= "";
                break;

            default:
                valor= "";
        }
        if (valor.length() > maxLargoCelda) {
            throw new CeldaDemasiadoLargaException(cell.getRowIndex()+1,cell.getColumnIndex()+1,valor.length());}
        return valor;
    }


    private boolean esZipBomb(Throwable exception) {
        Throwable actual = exception;

        while (actual != null) {
            if (actual.getMessage() != null &&
                    actual.getMessage().contains("Zip bomb detected")) {
                return true;
            }

            actual = actual.getCause();
        }

        return false;
    }

}
