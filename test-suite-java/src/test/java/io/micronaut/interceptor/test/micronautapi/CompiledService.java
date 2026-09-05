package io.micronaut.interceptor.test.micronautapi;

import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PostConstruct;

@Prototype
@Compiled(region = "users")
public class CompiledService {

    private final String name;

    public CompiledService(@Parameter String name) {
        this.name = name;
    }

    @PostConstruct
    void started() {
    }

    public String greet(String greeting) {
        return greeting + " " + name;
    }
}
