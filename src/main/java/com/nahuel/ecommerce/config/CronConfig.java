package com.nahuel.ecommerce.config;

import com.nahuel.ecommerce.entitys.Carrito;
import com.nahuel.ecommerce.entitys.EstadoOrden;
import com.nahuel.ecommerce.entitys.Usuario;
import com.nahuel.ecommerce.repositories.CarritoRepository;
import com.nahuel.ecommerce.repositories.OrdenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static com.nahuel.ecommerce.entitys.EstadoCarrito.ABANDONADO;

@Transactional
@Slf4j
@Component
@RequiredArgsConstructor
public class CronConfig {

    private final CarritoRepository carritoRepository;
    private final OrdenRepository ordenRepository;

    // Cada dos horas revisa las órdenes cuyo vencimiento ya se alcanzó.
    @Scheduled(cron = "0 0 */2 * * *", zone = "America/Argentina/Buenos_Aires")
    public void cancelarOrdenesVencidas() {
        int cantidad = ordenRepository.cancelarOrdenesVencidas(
                EstadoOrden.PENDIENTE_PAGO,
                EstadoOrden.CANCELADA,
                Instant.now()
        );
        log.info("Cron de vencimiento: {} órdenes canceladas", cantidad);
    }


    @Scheduled(fixedRate = 432000000L) // 5 días
    public void recordatorioCarritoAbandonado() {

        try {
            Instant limite = Instant.now().minus(Duration.ofDays(5));

            List<Carrito> carritos = carritoRepository.findCarritoUltimaInteraccion(limite);

            if (carritos.isEmpty()) {
                log.info("Job de recordatorio: no se encontraron carritos.");
                return;
            }

            for (Carrito carrito : carritos) {
                try {

                    if (carrito.getItems().isEmpty()) {
                        log.debug("Carrito {} sin items. Se omite.", carrito.getId());
                        continue;
                    }

                    Usuario usuario = carrito.getUsuario();

                    String mensaje = String.format(
                            "Hola %s, olvidaste %d producto(s) en tu carrito. ¡Todavía te están esperando!",
                            usuario.getNombreUsuario(),
                            carrito.getItems().size()
                    );

                    log.info("Enviando recordatorio al usuario {}: {}",
                            usuario.getId(), mensaje);

                } catch (Exception e) {
                    log.error("Error al procesar el carrito {}", carrito.getId(), e);
                }
            }

            log.info("Job finalizado. Carritos procesados: {}", carritos.size());

        } catch (Exception e) {
            log.error("Error al ejecutar el job de recordatorio.", e);
        }
    }

    // Se ejecuta los viernes a las 2:00 AM
    @Scheduled(cron = "0 0 2 * * FRI")
    public void eliminarCarritosAbandonados() {

        Instant limite = Instant.now().minus(Duration.ofDays(14));

        try {
            List<Carrito> carritosAbandonados =
                    carritoRepository.findCarritoEstadoCarritoAbandonado(ABANDONADO, limite);

            if (carritosAbandonados.isEmpty()) {
                log.info("Cron eliminarCarritosAbandonados: no hay carritos abandonados para eliminar (límite={})", limite);
                return;
            }

            log.info("Cron eliminarCarritosAbandonados: eliminando {} carritos abandonados (límite={})",
                    carritosAbandonados.size(), limite);

            for (Carrito carrito : carritosAbandonados) {
                try {
                    carritoRepository.delete(carrito);
                    log.debug("Carrito eliminado: {}", carrito.getId());
                } catch (Exception e) {
                    log.error("Error eliminando carrito {}", carrito.getId(), e);
                }
            }

            log.info("Cron eliminarCarritosAbandonados: finalizado.");

        } catch (Exception e) {
            log.error("Cron eliminarCarritosAbandonados: error ejecutando el job", e);
        }
    }


    @Scheduled(fixedRate = 864000000L) // 10 días
    public void marcarCarritosAbandonados() {

        Instant limite = Instant.now().minus(Duration.ofDays(10));

        try {
            List<Carrito> carritos =
                    carritoRepository.findCarritosActivosAbandonados(limite);

            if (carritos.isEmpty()) {
                log.info("No hay carritos para marcar como abandonados.");
                return;
            }

            log.info("Se encontraron {} carritos para marcar como abandonados.", carritos.size());

            for (Carrito carrito : carritos) {
                try {
                    carrito.setEstadoCarrito(ABANDONADO);
                    carritoRepository.save(carrito);

                    log.debug("Carrito {} marcado como abandonado.", carrito.getId());

                } catch (Exception e) {
                    log.error("Error marcando el carrito {} como abandonado.", carrito.getId(), e);
                }
            }

            log.info("Scheduler finalizado.");

        } catch (Exception e) {
            log.error("Error ejecutando el scheduler.", e);
        }
    }

}
