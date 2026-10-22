package com.nahuel.ecommerce.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class PasarelaPagoFakeConfig {
    @Bean
    @Primary
    PasarelaPagoFake pasarelaPagoFake() {
        return new PasarelaPagoFake();
    }
}
