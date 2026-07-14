package com.nahuel.ecommerce.config;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
@Slf4j
@Component
@RequiredArgsConstructor
public class CronConfig {

    private final CarritoRepository carritoRepository;


    @Scheduled(cron = "0 */2 * * * *")
    public void recordatorioCarritoAbandonado(){

        List<Carrito> carritos= carritoRepository.findCarritoUltimaInteraccion(Instant.now().minusSeconds(10*60));
        if (carritos.isEmpty()) {
            log.info("Job de limpieza finalizado: no se encontraron carritos");
            return;
        }

        carritos.forEach(carrito -> {
            try {
                System.out.println(
                        "hola "+
                        carrito.getUsuario().getId() + "dejaste abandonado tu carrito."
                );
                //log.debug("Producto eliminado - id={}, nombre={}", producto.getId(), producto.getNombre());
            } catch (Exception e) {
                //log.error("Error al eliminar producto id={}, nombre={}", producto.getId(), producto.getNombre(), e);
            }
        });

        //log.info("Job de limpieza finalizado: {} carritos", carritos.size());


    }


}
