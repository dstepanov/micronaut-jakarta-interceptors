package io.micronaut.interceptor.test.factorylifecycle;

/**
 * Produced by a field of the factory, declares no interception, and is final.
 */
public final class Gear implements AutoCloseable {

    public String name() {
        return "gear";
    }

    @Override
    public void close() {
    }
}
