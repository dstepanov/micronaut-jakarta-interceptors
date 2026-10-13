package io.micronaut.interceptor.test.micronautapi;

import jakarta.inject.Singleton;

/**
 * A class binding a value of its own, with a method that declares the binding again and leaves the value to its
 * default, one that declares nothing, and one overriding a method that declares a binding of its own.
 */
@Singleton
@Banded("class")
public class BandedService extends BandedBase {

    @Banded(note = "the method's")
    public String replaced() {
        return "replaced";
    }

    public String inherited() {
        return "inherited";
    }

    @Override
    public String overridden() {
        return "overridden";
    }
}
