package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Owned
public class OwnedPrototype {

    public String work() {
        return "done";
    }
}
