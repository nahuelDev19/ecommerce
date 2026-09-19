package com.nahuel.ecommerce.support;

import com.nahuel.ecommerce.entitys.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

/**
 * Fábrica de datos de prueba para construir entidades del dominio
 * sin lógica de negocio ni código de test (@Test).
 */
public class CarritoTestFactory {

    // Constantes de UUIDs reutilizables para garantizar consistencia en los tests
    public static final UUID CARRITO_ID_DEFAULT = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID ITEM_ID_DEFAULT    = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    public static final UUID PRODUCTO_ID_DEFAULT= UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    public static final UUID CUPON_ID_DEFAULT   = UUID.fromString("dddddddd-0000-0000-0000-000000000001");

    private CarritoTestFactory() {
        // Constructor privado para evitar instanciación
    }

    // =========================================================================
    // MÉTODOS PARA CARRITO
    // =========================================================================

    public static Carrito carritoVacio(Usuario duenio) {
        Carrito carrito = new Carrito();
        carrito.setId(CARRITO_ID_DEFAULT);
        carrito.setEstadoCarrito(EstadoCarrito.ACTIVO);
        carrito.setUsuario(duenio);
        carrito.setItems(new HashSet<>());
        carrito.setCupon(null);
        carrito.setCreadoEn(Instant.now());
        carrito.setActualizadoEn(Instant.now());
        carrito.setUltimaInteraccion(Instant.now());
        return carrito;
    }

    public static Carrito carritoCon(Usuario duenio, Producto producto, int cantidad, BigDecimal precioUnitario) {
        Carrito carrito = carritoVacio(duenio);

        ItemCarrito item = new ItemCarrito();
        item.setId(ITEM_ID_DEFAULT);
        item.setCarrito(carrito);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precioUnitario);
        item.setPrecioAgregadoEn(Instant.now());
        item.setActualizadoEn(Instant.now());

        carrito.getItems().add(item);
        return carrito;
    }

    // =========================================================================
    // MÉTODOS PARA CUPONES
    // =========================================================================

    public static Cupon cuponPorcentaje(BigDecimal porcentaje) {
        Instant ahora = Instant.now();
        return new Cupon(
                CUPON_ID_DEFAULT,
                "PORCENTAJE10",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                porcentaje,
                TipoMoneda.ARG, // Ajustar según enum TipoMoneda si aplica
                BigDecimal.ZERO,
                1,
                "Cupón de descuento porcentual vigente",
                ahora.minus(Duration.ofDays(1)),
                ahora.plus(Duration.ofDays(10))
        );
    }

    public static Cupon cuponMontoFijo(BigDecimal monto) {
        Instant ahora = Instant.now();
        return new Cupon(
                CUPON_ID_DEFAULT,
                "FIXED1000",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.MONTOFIJO,
                monto,
                TipoMoneda.ARG,
                BigDecimal.ZERO,
                1,
                "Cupón de monto fijo vigente",
                ahora.minus(Duration.ofDays(1)),
                ahora.plus(Duration.ofDays(10))
        );
    }

    public static Cupon cuponConMontoMinimo(BigDecimal porcentaje, BigDecimal montoMinimo) {
        Instant ahora = Instant.now();
        return new Cupon(
                CUPON_ID_DEFAULT,
                "MINIMO5000",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                porcentaje,
                TipoMoneda.ARG,
                montoMinimo,
                1,
                "Cupón con monto mínimo exigido",
                ahora.minus(Duration.ofDays(1)),
                ahora.plus(Duration.ofDays(10))
        );
    }

    public static Cupon cuponExpirado() {
        Instant ahora = Instant.now();
        return new Cupon(
                CUPON_ID_DEFAULT,
                "EXPIRADO",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                BigDecimal.valueOf(15),
                TipoMoneda.ARG,
                BigDecimal.ZERO,
                1,
                "Cupón cuya fecha de fin ya pasó",
                ahora.minus(Duration.ofDays(20)),
                ahora.minus(Duration.ofDays(5))
        );
    }

    public static Cupon cuponNoVigente() {
        Instant ahora = Instant.now();
        return new Cupon(
                CUPON_ID_DEFAULT,
                "FUTURO",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                BigDecimal.valueOf(20),
                TipoMoneda.ARG,
                BigDecimal.ZERO,
                1,
                "Cupón que aún no inicia su periodo de validez",
                ahora.plus(Duration.ofDays(5)),
                ahora.plus(Duration.ofDays(20))
        );
    }

    public static Cupon cuponInactivo() {
        Instant ahora = Instant.now();
        return new Cupon(
                CUPON_ID_DEFAULT,
                "INACTIVO",
                EstadoCupon.DESACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                BigDecimal.valueOf(10),
                TipoMoneda.ARG,
                BigDecimal.ZERO,
                1,
                "Cupón desactivado manualmente",
                ahora.minus(Duration.ofDays(1)),
                ahora.plus(Duration.ofDays(10))
        );
    }
}