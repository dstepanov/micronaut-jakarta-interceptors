package io.micronaut.interceptor.test.state;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Counted
public class CountedService {

    public String work() {
        return "done";
    }
}
