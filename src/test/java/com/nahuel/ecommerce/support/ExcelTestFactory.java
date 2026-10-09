package com.nahuel.ecommerce.support;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Fábrica de archivos Excel (.xlsx) en memoria, para usar en tests sin depender
 * de fixtures versionados en src/test/resources.
 *
 * El encabezado por defecto es el que exige el importer real
 * (ProcesarExcelService.validarColumna): nombre, descripcion, moneda, precio, activo.
 * La validación del importer es case-insensitive y no depende del orden, pero el
 * mapeo de los datos SÍ es posicional, así que respetá el orden del encabezado.
 *
 * Solo GENERA el archivo. No valida los datos de las filas a propósito: pasar
 * valores inválidos (precio "abc", moneda "XXX", celdas null) es válido y sirve
 * para testear el descarte de filas en el pipeline.
 *
 * Los fixtures de src/test/resources/fixtures/excel se conservan para los casos
 * que un archivo generado no puede representar (corrupto, .xls legacy, zip bomb).
 */
public final class ExcelTestFactory {

    /** Encabezado obligatorio, en el orden posicional que espera ProcesarExcelService. */
    public static final String[] ENCABEZADO_POR_DEFECTO = {
            "nombre", "descripcion", "moneda", "precio", "activo"
    };

    private ExcelTestFactory() {
        // Evita instanciar la factory
    }

    /**
     * Genera un .xlsx en memoria con el encabezado por defecto y las filas dadas.
     *
     * @param filas filas de datos (sin encabezado), en orden posicional.
     *              Un valor null deja la celda sin crear, que el importer lee como "".
     * @return el contenido del .xlsx listo para envolver en un MockMultipartFile
     *         o escribir en un Path temporal.
     */
    public static byte[] excel(Object[][] filas) {
        return excel(ENCABEZADO_POR_DEFECTO, filas);
    }

    /**
     * Igual que {@link #excel(Object[][])} pero con un encabezado propio, para
     * testear encabezados incorrectos (columnas faltantes, mal escritas, etc.).
     */
    public static byte[] excel(String[] encabezado, Object[][] filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Productos");

            Row filaEncabezado = sheet.createRow(0);
            for (int columna = 0; columna < encabezado.length; columna++) {
                filaEncabezado.createCell(columna).setCellValue(encabezado[columna]);
            }

            for (int indice = 0; indice < filas.length; indice++) {
                Row fila = sheet.createRow(indice + 1);
                Object[] datos = filas[indice];
                for (int columna = 0; columna < datos.length; columna++) {
                    if (datos[columna] != null) {
                        escribirCelda(fila.createCell(columna), datos[columna]);
                    }
                }
            }

            workbook.write(salida);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel en memoria", e);
        }

        return salida.toByteArray();
    }

    private static void escribirCelda(Cell celda, Object valor) {
        if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
        } else if (valor instanceof Boolean booleano) {
            celda.setCellValue(booleano);
        } else {
            celda.setCellValue(valor.toString());
        }
    }
}
