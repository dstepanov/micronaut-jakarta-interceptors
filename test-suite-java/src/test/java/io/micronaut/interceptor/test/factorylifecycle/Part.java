package io.micronaut.interceptor.test.factorylifecycle;

/**
 * Declares no interception, and is final, so a bean of it cannot be proxied either.
 */
public final class Part {

    public String name() {
        return "part";
    }
}
