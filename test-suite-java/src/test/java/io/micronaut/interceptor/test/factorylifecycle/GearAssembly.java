package io.micronaut.interceptor.test.factorylifecycle;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * A factory bound at its class whose bean is advised, as a bean in a normal scope is, and which produces a bean by a
 * field.
 */
@Factory
@Watched
@Around(proxyTarget = true)
public class GearAssembly {

    @Bean(preDestroy = "close")
    @Singleton
    Gear gear = new Gear();
}
