package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CuponDto;

import java.util.List;
import java.util.UUID;

public interface CuponeService {

    CuponDto crearCupon(CuponDto dto);
    List<CuponDto> listarCupones();
    List<CuponDto> listarCuponesPorEstadoActivo();
    List<CuponDto> listarCuponesPorEstadoDesactivo();
    void eliminarCuporPorId(UUID id);
    void eliminarDesactivos();
    CuponDto buscarCuponId(UUID id);


}
