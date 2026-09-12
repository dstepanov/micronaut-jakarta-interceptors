package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Rolled
public class RolledService {

    public String work() {
        return "done";
    }
}
