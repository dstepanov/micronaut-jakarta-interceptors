package io.micronaut.interceptor.test.reflection;

import jakarta.inject.Singleton;

@Singleton
@Measured
public class MeasuredService {

    public String work() {
        return "done";
    }
}
