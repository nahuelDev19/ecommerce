package com.nahuel.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync   // habilita el soporte de @Async en toda la app
public class AsyncConfig {

    // @Bean le dice a Spring: "creá este objeto una sola vez y dejalo disponible".
    // name = "importacionExecutor" es el nombre que vamos a usar en @Async("importacionExecutor").
    @Bean(name = "importacionExecutor")
    public Executor importacionExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();  // el pool de hilos

        // ---------- configuración del pool ----------

        executor.setCorePoolSize(2);   // 2 hilos SIEMPRE vivos, listos para importar
        executor.setMaxPoolSize(2);    // nunca más de 2 hilos
        executor.setQueueCapacity(20); // si ya hay 2 importando, hasta 20 esperan en fila

        // ---------- detalles útiles ----------

        executor.setThreadNamePrefix("importacion-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }


}