package io.micronaut.interceptor.test.handoff;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class HandedProductFactory {

    @Bean
    @Singleton
    @Handed
    HandedProduct product() {
        return new HandedProduct();
    }
}
