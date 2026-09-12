package io.micronaut.interceptor.test.handoff;

/** Produced by {@link HandedProductFactory}, which is what makes Micronaut proxy it with a separate target. */
public class HandedProduct {

    public String work() {
        return "done";
    }
}
