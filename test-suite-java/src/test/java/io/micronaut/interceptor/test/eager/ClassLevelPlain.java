package io.micronaut.interceptor.test.eager;

import io.micronaut.context.annotation.Prototype;

/** Bound on the class, so that its constructor is intercepted too, and proxied as a subclass. */
@Prototype
@Ordered
public class ClassLevelPlain {

    public ClassLevelPlain() {
        SubclassCreationTest.STEPS.add("object constructed");
    }

    public String work() {
        return "done";
    }
}
