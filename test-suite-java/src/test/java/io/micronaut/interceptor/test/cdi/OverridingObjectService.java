package io.micronaut.interceptor.test.cdi;

import jakarta.inject.Singleton;

/**
 * Bound as a class, and overriding the methods of {@code Object} as well as declaring overloads that only share
 * their names with them.
 */
@Singleton
@Secure
public class OverridingObjectService {

    @Override
    public String toString() {
        Calls.RECORDED.add("toString");
        return "service";
    }

    @Override
    public boolean equals(Object other) {
        Calls.RECORDED.add("equals");
        return this == other;
    }

    @Override
    public int hashCode() {
        Calls.RECORDED.add("hashCode");
        return 1;
    }

    public String toString(String prefix) {
        Calls.RECORDED.add("toString overload");
        return prefix + "service";
    }

    public boolean equals(OverridingObjectService other) {
        Calls.RECORDED.add("equals overload");
        return this == other;
    }

    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
