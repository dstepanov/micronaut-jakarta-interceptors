package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class ReporterFactory {

    @Bean
    @Singleton
    @Monitored // <1> the binding applies to the produced bean, as it does to a producer method in the specification
    Reporter reporter() {
        return new Reporter();
    }
}
