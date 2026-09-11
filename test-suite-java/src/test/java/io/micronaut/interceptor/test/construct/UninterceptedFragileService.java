package io.micronaut.interceptor.test.construct;

import io.micronaut.context.annotation.Prototype;

import java.io.IOException;

/**
 * The same constructor without interception, to hold the intercepted one to what Micronaut reports without it.
 */
@Prototype
public class UninterceptedFragileService {

    static volatile IOException failure;

    public UninterceptedFragileService() throws IOException {
        if (failure != null) {
            throw failure;
        }
    }
}
