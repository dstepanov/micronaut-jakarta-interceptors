package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
public class NulledService {

    /** A reference argument, which null is a value of. */
    @Nulled
    public String reference(String name) {
        return "reference " + name;
    }

    /** A primitive argument, which null is not a value of. */
    @Nulled
    public String primitive(int count) {
        return "primitive " + count;
    }
}
