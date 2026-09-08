package io.micronaut.interceptor.test.factory;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;

@Factory
public class DisposableFactory {

    /** A binding on the producing method makes the produced bean a proxy target. */
    @Bean(preDestroy = "close")
    @Prototype
    @Traced
    Disposable disposable() {
        return new Disposable();
    }
}
