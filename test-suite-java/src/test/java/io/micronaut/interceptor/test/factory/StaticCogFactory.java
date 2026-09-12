package io.micronaut.interceptor.test.factory;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;

@Factory
public class StaticCogFactory {

    @Prototype
    @Green
    static Cog cog() {
        return new Cog();
    }
}
