package io.micronaut.interceptor.test.state;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class ProducingFactory {

    @Bean
    @Singleton
    @Paired
    ProducedService produced() {
        return new ProducedService();
    }
}
