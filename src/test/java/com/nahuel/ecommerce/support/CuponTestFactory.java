package com.nahuel.ecommerce.support;

import com.nahuel.ecommerce.entitys.AlcanceCupon;
import com.nahuel.ecommerce.entitys.Cupon;
import com.nahuel.ecommerce.entitys.EstadoCupon;
import com.nahuel.ecommerce.entitys.TipoDescuento;
import com.nahuel.ecommerce.entitys.TipoMoneda;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class CuponTestFactory {

    private static final Instant FECHA_BASE =
            Instant.parse("2026-01-01T00:00:00Z");

    private CuponTestFactory() {
        // Evita instanciar la factory
    }

    /**
     * Cupón activo, vigente y con descuento porcentual.
     */
    public static Cupon crear() {
        return new Cupon(
                "DESC-10",
                "Cupón de descuento del 10%",
                EstadoCupon.ACTIVO,
                AlcanceCupon.POR_CARRITO,
                TipoDescuento.PORCENTAJE,
                BigDecimal.TEN,
                TipoMoneda.ARG,
                BigDecimal.ZERO,
                1,
                FECHA_BASE.minus(1, ChronoUnit.DAYS),
                FECHA_BASE.plus(30, ChronoUnit.DAYS)
        );
    }

    /**
     * Cupón con un UUID asignado.
     * Útil para tests que trabajan con entidades ya persistidas.
     */
    public static Cupon crearConId() {
        Cupon cupon = crear();
        cupon.setId(UUID.randomUUID());
        return cupon;
    }

    /**
     * Cupón activo y vigente con un código personalizado.
     */
    public static Cupon crearConCodigo(String codigo) {
        Cupon cupon = crear();
        cupon.setCodigoCupon(codigo);
        return cupon;
    }

    /**
     * Cupón porcentual con el valor indicado.
     */
    public static Cupon crearPorcentaje(BigDecimal porcentaje) {
        Cupon cupon = crear();
        cupon.setTipoDescuento(TipoDescuento.PORCENTAJE);
        cupon.setValorDescuento(porcentaje);
        return cupon;
    }

    /**
     * Cupón con descuento de monto fijo.
     */
    public static Cupon crearMontoFijo(BigDecimal monto) {
        Cupon cupon = crear();
        cupon.setCodigoCupon("MONTO-FIJO");
        cupon.setDescripcion("Cupón de descuento de monto fijo");
        cupon.setTipoDescuento(TipoDescuento.MONTOFIJO);
        cupon.setValorDescuento(monto);
        return cupon;
    }

    /**
     * Cupón desactivado.
     */
    public static Cupon crearDesactivado() {
        Cupon cupon = crear();
        cupon.setCodigoCupon("DESACTIVADO");
        cupon.setEstadoCupon(EstadoCupon.DESACTIVO);
        return cupon;
    }

    /**
     * Cupón cuya vigencia todavía no comenzó.
     */
    public static Cupon crearNoVigente() {
        Cupon cupon = crear();
        cupon.setCodigoCupon("NO-VIGENTE");
        cupon.setIniciadoEn(FECHA_BASE.plus(1, ChronoUnit.DAYS));
        cupon.setTerminaEn(FECHA_BASE.plus(30, ChronoUnit.DAYS));
        return cupon;
    }

    /**
     * Cupón expirado.
     */
    public static Cupon crearExpirado() {
        Cupon cupon = crear();
        cupon.setCodigoCupon("EXPIRADO");
        cupon.setIniciadoEn(FECHA_BASE.minus(30, ChronoUnit.DAYS));
        cupon.setTerminaEn(FECHA_BASE.minus(1, ChronoUnit.DAYS));
        return cupon;
    }

    /**
     * Cupón válido hasta el instante indicado.
     */
    public static Cupon crearVigenteHasta(Instant terminaEn) {
        Cupon cupon = crear();
        cupon.setCodigoCupon("VIGENTE-HASTA");
        cupon.setTerminaEn(terminaEn);
        return cupon;
    }

    /**
     * Cupón que comienza a estar vigente en el instante indicado.
     */
    public static Cupon crearVigenteDesde(Instant iniciadoEn) {
        Cupon cupon = crear();
        cupon.setCodigoCupon("VIGENTE-DESDE");
        cupon.setIniciadoEn(iniciadoEn);
        return cupon;
    }

    /**
     * Cupón aplicable por ítem.
     */
    public static Cupon crearPorItem() {
        Cupon cupon = crear();
        cupon.setCodigoCupon("POR-ITEM");
        cupon.setAlcanceCupon(AlcanceCupon.POR_ITEM);
        return cupon;
    }

    /**
     * Cupón aplicable al carrito completo.
     */
    public static Cupon crearPorCarrito() {
        Cupon cupon = crear();
        cupon.setCodigoCupon("POR-CARRITO");
        cupon.setAlcanceCupon(AlcanceCupon.POR_CARRITO);
        return cupon;
    }

    /**
     * Cupón aplicable a una categoría.
     */
    public static Cupon crearPorCategoria() {
        Cupon cupon = crear();
        cupon.setCodigoCupon("POR-CATEGORIA");
        cupon.setAlcanceCupon(AlcanceCupon.POR_CATEGORIA);
        return cupon;
    }

    /**
     * Cupón con monto mínimo de carrito.
     */
    public static Cupon crearConMontoMinimo(BigDecimal montoMinimo) {
        Cupon cupon = crear();
        cupon.setCodigoCupon("MINIMO-CARRITO");
        cupon.setMontoMinimoCarrito(montoMinimo);
        return cupon;
    }

    /**
     * Cupón con límite de uso por usuario.
     */
    public static Cupon crearConLimiteDeUso(Integer limite) {
        Cupon cupon = crear();
        cupon.setCodigoCupon("LIMITE-USO");
        cupon.setLimiteUsoPorUsuario(limite);
        return cupon;
    }

    /**
     * Cupón completo con todos los campos configurables.
     */
    public static Cupon crear(
            String codigoCupon,
            String descripcion,
            EstadoCupon estadoCupon,
            AlcanceCupon alcanceCupon,
            TipoDescuento tipoDescuento,
            BigDecimal valorDescuento,
            TipoMoneda moneda,
            BigDecimal montoMinimoCarrito,
            Integer limiteUsoPorUsuario,
            Instant iniciadoEn,
            Instant terminaEn
    ) {
        return new Cupon(
                codigoCupon,
                descripcion,
                estadoCupon,
                alcanceCupon,
                tipoDescuento,
                valorDescuento,
                moneda,
                montoMinimoCarrito,
                limiteUsoPorUsuario,
                iniciadoEn,
                terminaEn
        );
    }
}
