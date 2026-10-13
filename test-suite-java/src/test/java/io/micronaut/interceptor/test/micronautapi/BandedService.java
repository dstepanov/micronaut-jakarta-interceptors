package io.micronaut.interceptor.test.micronautapi;

import jakarta.inject.Singleton;

/**
 * A class binding a value of its own, with a method that declares the binding again and leaves the value to its
 * default, and one that declares nothing.
 */
@Singleton
@Banded("class")
public class BandedService {

    @Banded(note = "the method's")
    public String replaced() {
        return "replaced";
    }

    public String inherited() {
        return "inherited";
    }
}
