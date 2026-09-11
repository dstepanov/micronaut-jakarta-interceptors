package io.micronaut.interceptor.test.hierarchy;

/**
 * The superclass both intercepted beans inherit their business method from, without either of them overriding it.
 */
public class Workshop {

    public String work() {
        Hierarchy.CALLS.add("work");
        return "worked";
    }
}
