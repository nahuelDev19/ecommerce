package com.nahuel.ecommerce.mappers;

import com.nahuel.ecommerce.dtos.CuponDtoResponse;
import com.nahuel.ecommerce.entitys.Cupon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CuponMapper {

    public CuponDtoResponse toResponse(Cupon cupon) {
        CuponDtoResponse resp = new CuponDtoResponse();
        resp.setId(cupon.getId());
        resp.setCodigoCupon(cupon.getCodigoCupon());
        resp.setDescripcion(cupon.getDescripcion());
        resp.setEstadoCupon(cupon.getEstadoCupon());
        resp.setAlcanceCupon(cupon.getAlcanceCupon());
        resp.setTipoDescuento(cupon.getTipoDescuento());
        resp.setValorDescuento(cupon.getValorDescuento());
        resp.setMoneda(cupon.getMoneda());
        resp.setMontoMinimoCarrito(cupon.getMontoMinimoCarrito());
        resp.setLimiteUsoPorUsuario(cupon.getLimiteUsoPorUsuario());
        resp.setIniciadoEn(cupon.getIniciadoEn());
        resp.setTerminaEn(cupon.getTerminaEn());
        return resp;
    }

    public List<CuponDtoResponse> toResponse(List<Cupon> cupones) {
        return cupones.stream().map(this::toResponse).toList();
    }
}
