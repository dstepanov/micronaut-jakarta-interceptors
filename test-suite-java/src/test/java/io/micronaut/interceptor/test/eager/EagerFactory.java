package io.micronaut.interceptor.test.eager;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class EagerFactory {
    @Bean
    @Singleton
    MethodOnlyProduced methodOnly() { return new MethodOnlyProduced(); }

    @Bean
    @Singleton
    @Eager
    ClassLevelProduced classLevel() { return new ClassLevelProduced(); }
}
