package io.micronaut.interceptor.test.factory;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import jakarta.inject.Named;

@Factory
public class WidgetFactory {

    @Prototype
    @Named("green")
    @Green
    Widget green() {
        return new Widget();
    }

    @Prototype
    @Named("yellow")
    @Yellow
    Widget yellow() {
        return new Widget();
    }
}
