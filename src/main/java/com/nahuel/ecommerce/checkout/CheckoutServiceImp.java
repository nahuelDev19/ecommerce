package com.nahuel.ecommerce.checkout;

import com.nahuel.ecommerce.entitys.*;
import com.nahuel.ecommerce.exceptions.RecursoNoEncontradoException;
import com.nahuel.ecommerce.exceptions.ReglaNegocioException;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.OrdenRepository;
import com.nahuel.ecommerce.services.CalculadoraCarrito;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CheckoutServiceImp implements CheckoutService {

    private final CarritoRepository carritoRepository;
    private final CalculadoraCarrito calculadoraCarrito;
    private final OrdenRepository ordenRepository;

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
    @Transactional
    public String iniciarCheckout(UUID usuarioId) {
        Duration vigencia = Duration.parse(vigenciaOrden);
        if (vigencia.isZero() || vigencia.isNegative()) {
            throw new IllegalStateException("La vigencia de la orden debe ser positiva");
        }
        Carrito carrito= carritoRepository.findByUsuarioIdAndEstadoCarrito(usuarioId, EstadoCarrito.ACTIVO).orElseThrow(()-> new RecursoNoEncontradoException("El Carrito no fue encontrado o no esta activo"));
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
                monedaProducto = TipoMoneda.valueOf(item.getProducto().getMoneda());
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

        // Pendiente: persistir la orden y solicitar la URL mediante PasarelaPago.
        ordenRepository.save(orden);
        return "";
    }
}
