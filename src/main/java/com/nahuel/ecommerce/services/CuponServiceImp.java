package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.dtos.CuponDtoResponse;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.exceptions.*;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.CuponesRepository;
import com.nahuel.ecommerce.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.nahuel.ecommerce.entitys.EstadoCupon.ACTIVO;
import static com.nahuel.ecommerce.entitys.EstadoCupon.DESACTIVO;
import static com.nahuel.ecommerce.entitys.TipoDescuento.PORCENTAJE;

@Transactional
@Service
@RequiredArgsConstructor
public class CuponServiceImp implements CuponeService {

    private final CarritoRepository carritoRepository;
    private final CuponesRepository cuponesRepository;

    @Override
    public CuponDtoResponse crearCupon(CuponDto dto) {

        Instant inicio= dto.getIniciadoEn() == null ? Instant.now(): dto.getIniciadoEn();

        // validar para asegurar que terminado en ocurra despues de inicio
        if (dto.getTerminadoEn().compareTo(inicio) < 0) {
            throw new ReglaNegocioException("Terminado en no puede ser anterior a IniciadoEn");
        }

        // validar para asegurar que descuento del cupon no sea del 100% del valor total en porcentaje
        if(dto.getTipoDescuento()== PORCENTAJE && dto.getValorDescuento().compareTo(BigDecimal.valueOf(99))>0){
            throw new ReglaNegocioException("El porcentaje del cupon no debe ser mayor o igual a 100%");
        }

        cuponesRepository.findByCodigoCupon(dto.getCodigoCupon().toUpperCase())
                .ifPresent(cupon -> {
            throw new CuponYaExisteException(cupon.getCodigoCupon());
        });

        Cupon nuevoCupon = new Cupon(
                dto.getCodigoCupon().toUpperCase(),
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Cupon no encontrado: " + id));
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
    public boolean desactivarCupon(UUID id) {
        Cupon cupon = cuponesRepository.findById(id).orElseThrow(()-> new RecursoNoEncontradoException("id del cupon no encontrado"));
        cupon.setEstadoCupon(DESACTIVO);
        cuponesRepository.save(cupon);
        return true;
    }

    @Override
    public CuponDtoResponse buscarCuponId(UUID id) {
        Cupon cupon = cuponesRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cupon no encontrado: " + id));
        return cuponMapper(cupon);
    }

    //--------------------------------------

    @Override
    public CuponDtoResponse buscarPorCodigo(String codigo) {
        Cupon cupon= cuponesRepository.findByCodigoCupon(codigo.toUpperCase())
                .orElseThrow(()-> new RecursoNoEncontradoException("codigo no encontrado"));
        return cuponMapper(cupon);
    }

    @Override
    public CarritoDto aplicarCupon( UUID carritoId, String codCupon,UUID idUsuario) {
        Carrito carrito= carritoRepository.findByIdAndUsuarioId(carritoId,idUsuario)
                .orElseThrow(()-> new RecursoNoEncontradoException("carrito no encontrado"));
        Cupon cupon= cuponesRepository.findByCodigoCupon(codCupon.toUpperCase())
                .orElseThrow(()-> new RecursoNoEncontradoException("el cupon no fue encontrado"));

        validarAplicacionCupon(cupon,carrito);

        carrito.setCupon(cupon);
        carrito.setUltimaInteraccion(Instant.now());

        carritoRepository.save(carrito);
        return toDto(carrito);
    }

    private void validarAplicacionCupon(Cupon cupon, Carrito carrito ){
        Instant ahora = Instant.now();
        if (cupon.getEstadoCupon() != ACTIVO) {
            throw new CuponInvalidoException("El cupón no está activo");
        }
        if (ahora.isBefore(cupon.getIniciadoEn())) {
            throw new CuponNoVigenteException("El cupón todavía no está vigente");
        }
        if (ahora.isAfter(cupon.getTerminaEn())) {
            throw new CuponExpiradoException(cupon.getCodigoCupon());
        }
        if (carrito.getEstadoCarrito()==EstadoCarrito.ABANDONADO){
            throw new CarritoNoModificableException("El Carrito esta Desactivado");
        }
        if (carrito.getCupon()!=null){
            throw new CuponYaAplicadoException();
        }

        // validar casos en los que el cupon se deberia considera invalido para esta aplicacion

    }


    @Override
    public CarritoDto quitarCupon(UUID carritoId, UUID idUsuario) {
        Carrito carrito= carritoRepository.findByIdAndUsuarioId(carritoId, idUsuario)
                .orElseThrow(()-> new RecursoNoEncontradoException("carrito no encontrado"));
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




