package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;

/**
 * Two methods of the same name and the same number of arguments, bound to different interceptors.
 */
@Singleton
public class OverloadedService {

    @Alpha
    public String work(String value) {
        return "string " + value;
    }

    @Beta
    public String work(Integer value) {
        return "integer " + value;
    }
}
