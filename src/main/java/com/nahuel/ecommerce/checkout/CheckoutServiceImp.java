package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.dtos.CheckoutResponseDto;
import com.nahuel.ecommerce.dtos.OrdenDto;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.OrdenRepository;
import com.nahuel.ecommerce.repositories.PagoRepository;
import com.nahuel.ecommerce.services.CalculadoraCarrito;
import com.nahuel.ecommerce.mappers.OrdenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CheckoutServiceImp implements CheckoutService {

    private final CarritoRepository carritoRepository;
    private final CalculadoraCarrito calculadoraCarrito;
    private final OrdenRepository ordenRepository;
    private final PagoRepository pagoRepository;
    private final PasarelaPago pasarelaPago;
    private final OrdenMapper ordenMapper;

    @Value("${checkout.vigencia-orden}")
    private String vigenciaOrden;

    @Override
    @Transactional(readOnly = true)
    public EstadoOrden consultarEstadoOrden(UUID ordenId, UUID usuarioId) {
        Orden orden = ordenRepository.findByIdAndUsuarioId(ordenId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden no encontrada"));
        return orden.getEstado();
    }



    @Override
    @Transactional(readOnly = true)
    public OrdenDto consultarOrden(UUID ordenId, UUID usuarioId) {
        Orden orden = ordenRepository.findByIdAndUsuarioId(ordenId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden no encontrada"));
        return ordenMapper.toDto(orden);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenDto> listarOrdenes(UUID usuarioId) {
        return ordenMapper.toDto(ordenRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId));
    }

    @Override
    @Transactional
    public CheckoutResponseDto iniciarCheckout(UUID usuarioId) {
        Duration vigencia = Duration.parse(vigenciaOrden);
        if (vigencia.isZero() || vigencia.isNegative()) {
            throw new IllegalStateException("La vigencia de la orden debe ser positiva");
        }
        // El bloqueo dura hasta el commit/rollback: otro checkout del mismo
        // carrito espera y vuelve a evaluar si sigue ACTIVO después del primero.
        Carrito carrito = carritoRepository.buscarActivoParaCheckout(usuarioId, EstadoCarrito.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El Carrito no fue encontrado o no esta activo"));
        if(carrito.getItems().isEmpty()){
            throw new ReglaNegocioException("No se puede iniciar una compra con un carrito vacío");
        }
        Orden orden= new Orden();
        orden.setUsuario(carrito.getUsuario());

        Instant momentoActual= Instant.now();
        BigDecimal subtotal = BigDecimal.ZERO;
        TipoMoneda monedaOrden = null;

        for (ItemCarrito item: carrito.getItems()){
            TipoMoneda monedaProducto;
            try {
                String codigoMoneda = item.getProducto().getMoneda();
                // El catálogo usa ARS, pero nuestro enum todavía representa pesos como ARG.
                // Adaptamos ese código al leer el producto, sin cambiar los datos existentes.
                // El adapter y el webhook ya traducen ARG a ARS al comunicarse con Mercado Pago.
                monedaProducto = "ARS".equals(codigoMoneda)
                        ? TipoMoneda.ARG
                        : TipoMoneda.valueOf(codigoMoneda);
            } catch (IllegalArgumentException | NullPointerException ex) {
                throw new ReglaNegocioException("El producto tiene una moneda no válida");
            }
            if (monedaOrden == null) {
                monedaOrden = monedaProducto;
            } else if (monedaOrden != monedaProducto) {
                throw new ReglaNegocioException("No se puede comprar en distintas monedas en una misma orden");
            }

            ItemOrden itemOrden = new ItemOrden();
            itemOrden.setOrden(orden);
            itemOrden.setPrecioUnitario(item.precioVigente(momentoActual));
            itemOrden.setCantidad(item.getCantidad());
            itemOrden.setProductoId(item.getProducto().getId());
            itemOrden.setNombreProducto(item.getProducto().getNombre());
            orden.getItems().add(itemOrden);
            orden.setEstado(EstadoOrden.PENDIENTE_PAGO);
            subtotal = subtotal.add(itemOrden.getSubtotal());
        }

        BigDecimal descuento = calculadoraCarrito.descuento(carrito.getCupon(), subtotal);
        orden.setSubtotal(subtotal);
        orden.setDescuentoTotal(descuento);
        orden.setTotal(subtotal.subtract(descuento));
        orden.setMoneda(monedaOrden);
        orden.setCreadoEn(momentoActual);
        orden.setVenceEn(momentoActual.plus(vigencia));
        orden.setActualizadoEn(momentoActual);

        // El ID generado identifica esta compra en la preferencia y en el webhook.
        ordenRepository.save(orden);

        // El intento local existe antes de solicitar la preferencia, pero todavía
        // no tiene ID de pago externo: ese ID se conoce cuando hay un cobro real.
        Pago pago = new Pago();
        pago.setOrden(orden);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setMonto(orden.getTotal());
        pago.setMoneda(orden.getMoneda());
        pago.setCreadoEn(momentoActual);
        pago.setActualizadoEn(momentoActual);
        pagoRepository.saveAndFlush(pago);

        // El adapter obtiene el enlace; crear la preferencia no aprueba la orden.
        // No capturamos el fallo: al propagarse, @Transactional revierte orden,
        // ítems y pago locales, incluso si ya se enviaron los INSERT a la base.
        PreferenciaPago preferencia = pasarelaPago.crearPreferencia(orden);
        pago.setPreferenciaId(preferencia.preferenciaId());
        pago.setUrlPago(preferencia.urlPago());
        pago.setActualizadoEn(Instant.now());
        pagoRepository.save(pago);

        // Decisión: conservamos los ítems y consumimos el carrito como ORDENADO.
        // Significa que originó una orden, no que esté pagada. Un segundo clic
        // ya no puede reutilizarlo; si falla esta transacción, sigue ACTIVO.
        carrito.setEstadoCarrito(EstadoCarrito.ORDENADO);
        carrito.setActualizadoEn(Instant.now());
        carritoRepository.save(carrito);
        return new CheckoutResponseDto(orden.getId(), preferencia.urlPago());
    }
}
