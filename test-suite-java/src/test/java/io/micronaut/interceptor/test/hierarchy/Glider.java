package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

@Singleton
@Winged
public class Glider extends Vehicle {

    @PostConstruct
    public void initGlider() {
        Hierarchy.CALLS.add("glider");
    }
}
