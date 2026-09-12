package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Scoped
public class ScopedUsingPrototype {

    public String work() {
        return "done";
    }
}
