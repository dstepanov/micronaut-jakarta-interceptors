package io.micronaut.interceptor.test.reflection;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class ProducedServiceFactory {

    @Singleton
    @Produced
    ProducedService producedService() {
        return new ProducedService();
    }
}
