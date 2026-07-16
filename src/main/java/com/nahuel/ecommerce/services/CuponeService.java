package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.dtos.CuponDtoResponse;

import java.util.List;
import java.util.UUID;

public interface CuponeService {

    CuponDtoResponse crearCupon(CuponDto dto);
    List<CuponDtoResponse> listarCupones();
    List<CuponDtoResponse> listarCuponesPorEstadoActivo();
    List<CuponDtoResponse> listarCuponesPorEstadoDesactivo();
    boolean eliminarCuponPorId(UUID id);
    boolean eliminarDesactivos();
    boolean desactivarCupon();
    CuponDtoResponse buscarCuponId(UUID id);

    CuponDtoResponse buscarPorCodigo(String codigo);
    CarritoDto aplicarCupon(UUID carritoId, String codigoCupon);
    CarritoDto quitarCupon(UUID carritoId);
}
