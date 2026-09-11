package io.micronaut.interceptor.test.binding;

import jakarta.inject.Singleton;

@Singleton
@Zone("a")
public class ZonedService {

    public String inherited() {
        return "inherited";
    }

    /**
     * Declares the binding of its class again, leaving the member to its default, which replaces the class's
     * {@code @Zone("a")} with {@code @Zone("z")}.
     */
    @Zone
    public String replacedByDefault() {
        return "by default";
    }

    @Zone("z")
    public String replacedSpeltOut() {
        return "spelt out";
    }
}
