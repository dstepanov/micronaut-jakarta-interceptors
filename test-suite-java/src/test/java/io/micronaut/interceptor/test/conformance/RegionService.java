package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
@Region("a")
public class RegionService {

    public String fromTheClass() {
        return "a";
    }

    @Region("b") // <1> replaces the binding of the same type declared at class level
    public String fromTheMethod() {
        return "b";
    }
}
