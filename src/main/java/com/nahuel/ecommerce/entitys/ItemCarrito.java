package com.nahuel.ecommerce.entitys;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@Entity
public class ItemCarrito {

    public static final Duration VIGENCIA_PRECIO = Duration.ofHours(48);



    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id")
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private Integer cantidad;

    private BigDecimal precioUnitario;

    private Instant precioAgregadoEn;

    private Instant actualizadoEn;


    public boolean precioCongeladoVigente(Instant ahora){
        if(precioAgregadoEn==null){
            return true;
        }
        return ahora.isBefore(precioAgregadoEn.plus(VIGENCIA_PRECIO));
    }


    public BigDecimal precioVigente(Instant ahora){
        return precioCongeladoVigente(ahora)?
                precioUnitario:
                producto.getPrecioBase();
    }

    public void  congelarPrecio(BigDecimal precio,Instant ahora){
        this.precioUnitario=precio;
        this.precioAgregadoEn=ahora;
    }

}
