package io.micronaut.interceptor.test.micronautapi;

/**
 * A method declaring a binding of its own, which an override that declares nothing does not have.
 */
public class BandedBase {

    @Banded(value = "base", note = "the base's")
    public String overridden() {
        return "base";
    }
}
