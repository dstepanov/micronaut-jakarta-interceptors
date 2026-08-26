package io.micronaut.interceptor.test.concurrency;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Concurrent
public class ConcurrentPrototype {

    public String echo(String value) {
        return "echo " + value;
    }
}
