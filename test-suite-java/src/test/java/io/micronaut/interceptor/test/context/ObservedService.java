package io.micronaut.interceptor.test.context;

import jakarta.inject.Singleton;

@Singleton
@Observed(label = "service")
public class ObservedService {

    public String greet(String name) {
        return "Hello " + name;
    }
}
