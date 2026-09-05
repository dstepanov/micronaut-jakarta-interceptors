package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;

/**
 * The superclass both intercepted beans inherit their first {@code @PostConstruct} callback from.
 */
public class Vehicle {

    @PostConstruct
    public void initVehicle() {
        Hierarchy.CALLS.add("vehicle");
    }
}
