package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Fragile
public class FragileService {

    public String work() {
        return "done";
    }
}
