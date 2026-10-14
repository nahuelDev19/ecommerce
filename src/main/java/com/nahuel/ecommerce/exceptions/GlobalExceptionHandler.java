package com.nahuel.ecommerce.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size}")
    private DataSize maxFileSize;

    // Captura TODAS tus excepciones de negocio de una sola vez
    @ExceptionHandler(AppException.class)
    public ProblemDetail handleAppException(AppException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getCode());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    // Archivo duplicado: devuelve el id de la importación anterior
    @ExceptionHandler(ArchivoDuplicadoException.class)
    public ProblemDetail handleArchivoDuplicado(ArchivoDuplicadoException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getCode());
        problem.setProperty("importacionId", ex.getImportacionAnterior());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    // Errores de validación (@Valid en DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Error de validación");
        problem.setTitle("VALIDATION_ERROR");
        problem.setProperty("errores", errores);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // Devuelve HTTP 401
                .body(Map.of("error", ex.getMessage())); // Devuelve JSON: {"error": "username o password incorrectos"}
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> manejarArchivoGrande(MaxUploadSizeExceededException e){

        long limite = e.getMaxUploadSize()>0? e.getMaxUploadSize() : maxFileSize.toMegabytes();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONTENT_TOO_LARGE,"El archivo supera el tamaño máximo permitido de " + limite + " MB");
        problemDetail.setTitle("Archivo demasiado grande");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("maxUploadSize",limite + " MB");

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(problemDetail) ;
    }


    @ExceptionHandler(OutOfMemoryError.class)
    public ResponseEntity<ProblemDetail> handleOutOfMemoryError(OutOfMemoryError ex){
        log.error("OutOfMemoryError capturado: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(
                HttpStatus.CONTENT_TOO_LARGE,
                "El archivo es demasiado grande para procesar"
        );
        problemDetail.setTitle("SIN_MEMORIA");
        problemDetail.setProperty("timestamp",Instant.now());
        return  ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(problemDetail);
    }

}
