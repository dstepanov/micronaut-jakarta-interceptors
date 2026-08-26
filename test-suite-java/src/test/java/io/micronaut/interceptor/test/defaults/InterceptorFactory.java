package io.micronaut.interceptor.test.defaults;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class InterceptorFactory {

    @Bean
    @Singleton
    FactoryMadeInterceptor factoryMade() {
        return new FactoryMadeInterceptor("configured at runtime");
    }
}
