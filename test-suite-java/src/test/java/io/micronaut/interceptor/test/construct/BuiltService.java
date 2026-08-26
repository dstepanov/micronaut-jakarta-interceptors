package io.micronaut.interceptor.test.construct;

import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;

@Prototype
@Built
public class BuiltService {

    private final String name;

    public BuiltService(@Parameter String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
