package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Shared
public class SharingPrototype {

    public String work() {
        return "done";
    }
}
