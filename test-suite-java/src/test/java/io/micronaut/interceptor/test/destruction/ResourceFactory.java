package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/** A singleton factory, which is what a factory usually is. */
@Factory
public class ResourceFactory {

    @Bean
    @Singleton
    @Produced
    ProducedResource resource() {
        return new ProducedResource();
    }
}
