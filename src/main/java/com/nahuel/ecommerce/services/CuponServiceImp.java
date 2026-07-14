package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.repositories.CuponesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class CuponServiceImp implements CuponeService {

    private final CuponesRepository cuponesRepository;

    @Override
    public CuponDto crearCupon(CuponDto dto) {
        Cupon nuevoCupon = new Cupon(
                dto.getId(), // si querés permitir pasar id; si no, mandá null
                dto.getCodigoCupon(),
                dto.getActivo(),
                dto.getAlcanceCupon(),
                dto.getTipoDescuento(),
                dto.getValorDescuento(),
                dto.getMoneda(),
                dto.getMontoMinimoCarrito(),
                dto.getLimiteUsoTotal(),
                dto.getLimiteUsoPorUsuario(),
                dto.getIniciadoEn(),
                dto.getTerminaEn()
        );

        Cupon cuponGuardado = cuponesRepository.save(nuevoCupon);
        return productoMapper(cuponGuardado);
    }


    @Override
    public List<CuponDto> listarCupones() {
        return cuponesRepository.findAll()
                .stream()
                .map(this::productoMapper)
                .collect(Collectors.toList());
    }

    @Override
    public List<CuponDto> listarCuponesPorEstadoActivo() {
        return cuponesRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .map(this::productoMapper)
                .collect(Collectors.toList());
    }@Override
    public List<CuponDto> listarCuponesPorEstadoDesactivo() {
        return cuponesRepository.findAll().stream()
                .filter(c -> Boolean.FALSE.equals(c.getActivo()))
                .map(this::productoMapper)
                .collect(Collectors.toList());
    }

    @Override
    public boolean eliminarCuponPorId(UUID id) {
        Cupon cupon = cuponesRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cupon no encontrado: " + id));
        cuponesRepository.delete(cupon);
        return false;
    }

    @Override
    public boolean eliminarDesactivos() {
        cuponesRepository.deleteByActivoFalse();
        return true;
    }

    @Override
    public boolean desactivarCupon() {
        return false;
    }

    @Override
    public CuponDto buscarCuponId(UUID id) {
        Cupon cupon = cuponesRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cupon no encontrado: " + id));
        return productoMapper(cupon);
    }


    private CuponDto productoMapper(Cupon cupon) {
        return new CuponDto(
                cupon.getId(),
                cupon.getCodigoCupon(),
                cupon.getActivo(),
                cupon.getAlcanceCupon(),
                cupon.getTipoDescuento(),
                cupon.getValorDescuento(),
                cupon.getMoneda(),
                cupon.getMontoMinimoCarrito(),
                cupon.getLimiteUsoTotal(),
                cupon.getLimiteUsoPorUsuario(),
                cupon.getIniciadoEn(),
                cupon.getTerminaEn()
        );
    }



}
