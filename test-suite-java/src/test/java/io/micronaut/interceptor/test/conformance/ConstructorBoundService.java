package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
public class ConstructorBoundService {

    @BoundToConstructor // <1> the binding is declared on the constructor, not on the class
    public ConstructorBoundService() {
        Calls.RECORDED.add("constructor");
    }

    public String work() {
        return "done";
    }
}
