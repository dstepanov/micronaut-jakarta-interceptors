package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

@Singleton
@Armed
public class Tank extends Vehicle {

    @PostConstruct
    public void initTank() {
        Hierarchy.CALLS.add("tank");
    }
}
