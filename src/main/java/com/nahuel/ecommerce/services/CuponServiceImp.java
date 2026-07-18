package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.dtos.CuponDtoResponse;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.entitys.EstadoCupon;
import com.nahuel.ecommerce.entitys.ItemCarrito;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.CuponesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nahuel.ecommerce.entitys.EstadoCupon.ACTIVO;
import static com.nahuel.ecommerce.entitys.EstadoCupon.DESACTIVO;

@Transactional
@Service
@RequiredArgsConstructor
public class CuponServiceImp implements CuponeService {

    private final CarritoRepository carritoRepository;
    private final CuponesRepository cuponesRepository;

    @Override
    public CuponDtoResponse crearCupon(CuponDto dto) {

        Instant inicio= dto.getIniciadoEn() == null ? dto.getIniciadoEn():Instant.now();
        // validar para verificar que terminado en no sea null
        // validar para asegurar que terminado en ocurra despues de inicio
        // validar valor decuento para que no sea nulo o 0
        // validar para asegurar que descuento del cupon no sea del 100% del valor total en porcentaje

        cuponesRepository.findByCodigoCupon(dto.getCodigoCupon().toUpperCase()).ifPresent(cupon -> {
            throw new RuntimeException("ya existe un cupon con este codigo");
        });

        Cupon nuevoCupon = new Cupon(
                dto.getCodigoCupon(),
                dto.getDescripcion(),
                EstadoCupon.ACTIVO,
                dto.getAlcanceCupon(),
                dto.getTipoDescuento(),
                dto.getValorDescuento(),
                dto.getMoneda(),
                dto.getMontoMinimoCarrito(),
                dto.getLimiteUsoPorUsuario(),
                inicio,
                dto.getTerminadoEn()
        );

        Cupon cuponGuardado = cuponesRepository.save(nuevoCupon);
        return cuponMapper(cuponGuardado);
    }


    @Override
    public List<CuponDtoResponse> listarCupones() {
        return cuponesRepository.findAll()
                .stream()
                .map(this::cuponMapper)
                .collect(Collectors.toList());
    }

    @Override
    public List<CuponDtoResponse> listarCuponesPorEstadoActivo() {
        return cuponesRepository.findAll().stream()
                .filter(c -> c.getEstadoCupon() == ACTIVO)
                .map(this::cuponMapper)
                .collect(Collectors.toList());
    }

    @Override
    public List<CuponDtoResponse> listarCuponesPorEstadoDesactivo() {
        return cuponesRepository.findAll().stream()
                .filter(c -> c.getEstadoCupon()== DESACTIVO)
                .map(this::cuponMapper)
                .collect(Collectors.toList());
    }

    @Override
    public boolean eliminarCuponPorId(UUID id) {
        Cupon cupon = cuponesRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cupon no encontrado: " + id));
        cuponesRepository.delete(cupon);
        return true;
    }

    @Override
    public boolean eliminarDesactivos() {
        //cuponesRepository.deleteByActivoFalse();
        List<Cupon> desactivados= cuponesRepository.findByEstadoCupon(DESACTIVO);
        cuponesRepository.deleteAll(desactivados);
        return !desactivados.isEmpty();
    }

    @Override
    public boolean desactivarCupon() {
        return true;
    }

    @Override
    public CuponDtoResponse buscarCuponId(UUID id) {
        Cupon cupon = cuponesRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Cupon no encontrado: " + id));
        return cuponMapper(cupon);
    }

    //--------------------------------------

    @Override
    public CuponDtoResponse buscarPorCodigo(String codigo) {
        Cupon cupon= cuponesRepository.findByCodigoCupon(codigo.toUpperCase()).orElseThrow(()-> new RuntimeException("codigo no encontrado"));
        return cuponMapper(cupon);
    }

    @Override
    public CarritoDto aplicarCupon(UUID carritoId, String codigoCupon) {
        Carrito carrito= carritoRepository.findById(carritoId)
                .orElseThrow(()-> new RuntimeException("carrito no encontrado"));
        Cupon cupon= cuponesRepository.findByCodigoCupon(codigoCupon)
                .orElseThrow(()-> new RuntimeException("el cupon no fue encontrado"));

        // validar cupon (cupon, carrito) retorna exceptions si el cupon no es valido
        carrito.setCupon(cupon);
        carrito.setUltimaInteraccion(Instant.now());

        carritoRepository.save(carrito);
        return toDto(carrito);
    }

    private void validarCupon(Cupon cupon, Carrito carrito ){
        Instant ahora = Instant.now();
        // validar casos en los que el cupon se deberia considera invalido para esta aplicacion
    }


    @Override
    public CarritoDto quitarCupon(UUID carritoId) {
        Carrito carrito= carritoRepository.findById(carritoId).orElseThrow(()-> new RuntimeException("carrito no encontrado"));
        carrito.setCupon(null);
        carrito.setUltimaInteraccion(Instant.now());
        carritoRepository.save(carrito);
        return toDto(carrito);
    }


    private CuponDtoResponse cuponMapper(Cupon cupon) {
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
/*
    private CarritoDto toDto(Carrito carrito) {

        CarritoDto dto = new CarritoDto();

        dto.setId(carrito.getId());
        dto.setEstadoCarrito(carrito.getEstadoCarrito());
        dto.setUsuarioId(carrito.getUsuario().getId());

        dto.setItems(new ArrayList<>());
        dto.setSubtotal(BigDecimal.ZERO);
        dto.setDescuentoTotal(BigDecimal.ZERO);
        dto.setTotal(BigDecimal.ZERO );

        return dto;
    }
*/
private CarritoDto toDto(Carrito carrito) {

    CarritoDto dto = new CarritoDto();

    BigDecimal subtotal = calcularSubtotal(carrito);
    BigDecimal descuento = calcularDescuento(carrito, subtotal);
    BigDecimal total = subtotal.subtract(descuento);

    dto.setId(carrito.getId());
    dto.setEstadoCarrito(carrito.getEstadoCarrito());
    dto.setUsuarioId(carrito.getUsuario().getId());
    dto.setCreadoEn(carrito.getCreadoEn());
    dto.setActualizadoEn(carrito.getActualizadoEn());
    dto.setUltimaInteraccion(carrito.getUltimaInteraccion());

    dto.setItems(
            carrito.getItems()
                    .stream()
                    .map(this::toDto)
                    .toList()
    );

    dto.setSubtotal(subtotal);
    dto.setDescuentoTotal(descuento);
    dto.setTotal(total);

    return dto;
}
    private BigDecimal calcularSubtotal(Carrito carrito) {
        return carrito.getItems().stream()
                .map(item -> item.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(item.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    static BigDecimal calcularDescuento(Carrito carrito, BigDecimal subtotal) {

        if (carrito.getCupon() == null) {
            return BigDecimal.ZERO;
        }

        Cupon cupon = carrito.getCupon();

        switch (cupon.getTipoDescuento()) {

            case PORCENTAJE:
                return subtotal
                        .multiply(cupon.getValorDescuento())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            case MONTOFIJO:
                return cupon.getValorDescuento().min(subtotal);

            default:
                return BigDecimal.ZERO;
        }
    }
    private ItemCarritoDto toDto(ItemCarrito item) {

        ItemCarritoDto dto = new ItemCarritoDto();

        dto.setId(item.getId());
        dto.setProductoId(item.getProducto().getId());
        dto.setNombreProducto(item.getProducto().getNombre());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setCantidad(item.getCantidad());
        dto.setSubtotal(
                item.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(item.getCantidad()))
        );

        return dto;
    }

}




