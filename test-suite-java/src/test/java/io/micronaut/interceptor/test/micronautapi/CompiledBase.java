package io.micronaut.interceptor.test.micronautapi;

import jakarta.annotation.PostConstruct;

/** A superclass declaring a callback of its own, which the event of a subclass invokes first. */
public class CompiledBase {

    @PostConstruct
    void startedBase() {
    }
}
