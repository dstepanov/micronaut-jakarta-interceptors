package io.micronaut.interceptor.test.hierarchy;

import jakarta.inject.Singleton;

@Singleton
@Layered
public class LayeredService {

    public String work() {
        Hierarchy.CALLS.add("work");
        return "worked";
    }
}
