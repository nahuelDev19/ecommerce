package com.nahuel.ecommerce.services;

import com.nahuel.ecommerce.dtos.CarritoDto;
import com.nahuel.ecommerce.dtos.CuponDto;
import com.nahuel.ecommerce.dtos.CuponDtoResponse;
import com.nahuel.ecommerce.dtos.ItemCarritoDto;
import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.exceptions.*;
import com.nahuel.ecommerce.mappers.CarritoMapper;
import com.nahuel.ecommerce.mappers.CuponMapper;
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
    private final CarritoMapper carritoMapper;
    private final CuponMapper cuponMapper;


    @Override
    public CuponDtoResponse crearCupon(CuponDto dto) {

        Instant inicio= dto.getIniciadoEn() == null ? Instant.now(): dto.getIniciadoEn();
        if (dto.getTerminadoEn() == null) {
            throw new ReglaNegocioException("La fecha de finalización es obligatoria");
        }

        if (dto.getValorDescuento() == null
                || dto.getValorDescuento().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El valor del descuento debe ser mayor a 0");
        }
        // validar para asegurar que terminado en ocurra despues de inicio
        if (dto.getTerminadoEn().compareTo(inicio) < 0 ) {
            throw new ReglaNegocioException("Terminado en no puede ser anterior a IniciadoEn");
        }

        // validar para asegurar que descuento del cupon no sea del 100% del valor total en porcentaje
        if(dto.getTipoDescuento()== PORCENTAJE && dto.getValorDescuento().compareTo(BigDecimal.valueOf(100))>=0){
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
        return cuponMapper.toResponse(cuponGuardado);
    }


    @Override
    public List<CuponDtoResponse> listarCupones() {
        return cuponesRepository.findAll()
                .stream()
                .map(cuponMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CuponDtoResponse> listarCuponesPorEstadoActivo() {
        return cuponesRepository.findAll().stream()
                .filter(c -> c.getEstadoCupon() == ACTIVO)
                .map(cuponMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CuponDtoResponse> listarCuponesPorEstadoDesactivo() {
        return cuponesRepository.findAll().stream()
                .filter(c -> c.getEstadoCupon()== DESACTIVO)
                .map(cuponMapper::toResponse)
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
        return cuponMapper.toResponse(cupon);
    }

    //--------------------------------------

    @Override
    public CuponDtoResponse buscarPorCodigo(String codigo) {
        Cupon cupon= cuponesRepository.findByCodigoCupon(codigo.toUpperCase())
                .orElseThrow(()-> new RecursoNoEncontradoException("codigo no encontrado"));
        return cuponMapper.toResponse(cupon);
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
        return carritoMapper.toDto(carrito);
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
        if (carrito.getEstadoCarrito()!=EstadoCarrito.ACTIVO){
            throw new CarritoNoModificableException("Solo un carrito ACTIVO puede recibir un cupón");
        }
        if (carrito.getCupon()!=null){
            throw new CuponYaAplicadoException();
        }
        if(carrito.getItems().isEmpty()){
            throw new ReglaNegocioException("los items no pueden estar vacios");
        }
        if (cupon.getMontoMinimoCarrito() != null &&
                cupon.getMontoMinimoCarrito().compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal subtotal = carritoMapper.toDto(carrito).getSubtotal();
            if (subtotal.compareTo(cupon.getMontoMinimoCarrito()) < 0) {
                throw new MontoMinimoNoAlcanzadoException("El subtotal del carrito no alcanza el monto mínimo requerido para usar este cupón.");            }
        }
    }


    @Override
    public CarritoDto quitarCupon(UUID carritoId, UUID idUsuario) {
        Carrito carrito= carritoRepository.findByIdAndUsuarioId(carritoId, idUsuario)
                .orElseThrow(()-> new RecursoNoEncontradoException("carrito no encontrado"));
        carrito.setCupon(null);
        carrito.setUltimaInteraccion(Instant.now());
        carritoRepository.save(carrito);
        return carritoMapper.toDto(carrito);
    }





}




