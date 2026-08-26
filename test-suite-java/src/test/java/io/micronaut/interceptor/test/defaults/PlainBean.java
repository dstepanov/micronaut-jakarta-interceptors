package io.micronaut.interceptor.test.defaults;

import jakarta.inject.Singleton;

/** No jakarta interceptor annotation at all. */
@Singleton
public class PlainBean {

    public String work() {
        return "plain";
    }
}
