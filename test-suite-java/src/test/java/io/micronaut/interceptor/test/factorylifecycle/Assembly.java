package io.micronaut.interceptor.test.factorylifecycle;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * A factory bound at its class: the binding is the factory's, not the one of the bean it produces.
 */
@Factory
@Watched
public class Assembly {

    @Bean
    @Singleton
    Part make() {
        return new Part();
    }
}
