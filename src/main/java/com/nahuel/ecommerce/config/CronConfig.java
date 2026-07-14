package com.nahuel.ecommerce.config;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.Usuario;
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

        try {

        List<Carrito> carritos= carritoRepository.findCarritoUltimaInteraccion(Instant.now().minusSeconds(10*60));
        if (carritos==null || carritos.isEmpty()) {
            log.info("Job de recordatorio finalizado: no se encontraron carritos");
            return;
        }

        carritos.forEach(carrito -> {
            try {
                Usuario usuario= carrito.getUsuario();
                String mensaje = String.format(
                        "Hola %s, olvidaste %d producto(s) en tu carrito. ¡Todavía te están esperando!",
                        usuario.getNombreUsuario(),
                        carrito.getItems().size()
                );
                System.out.println(mensaje);
                log.info("Enviando recordatorio al usuario {}: {}",
                        usuario.getId(),
                        mensaje);
            } catch (Exception e) {
                log.error("Error al procesar el carrito {}",
                        carrito.getId(), e);            }
        });

        log.info("Job finalizado. Carritos procesados: {}", carritos.size());
        }catch (Exception e){
            log.error("Error al ejecutar el job de recordatorio de carrito abandonado.", e);

        }


    }


}
