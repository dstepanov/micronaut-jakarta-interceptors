package io.micronaut.interceptor.test

import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Prototype
import jakarta.annotation.PostConstruct

@Prototype
@Compiled(region = "users")
class CompiledService {

    private final String name

    CompiledService(@Parameter String name) {
        this.name = name
    }

    @PostConstruct
    void started() {
    }

    String greet(String greeting) {
        "$greeting $name"
    }
}
