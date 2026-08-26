package io.micronaut.interceptor.test.defaults;

import jakarta.inject.Singleton;

@Singleton
@Marked
public class MarkedBean {

    public String work() {
        return "marked";
    }
}
