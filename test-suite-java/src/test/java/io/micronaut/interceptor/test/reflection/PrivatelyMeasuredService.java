package io.micronaut.interceptor.test.reflection;

import jakarta.inject.Singleton;

@Singleton
@PrivatelyMeasured
public class PrivatelyMeasuredService {

    public String work() {
        return "done";
    }
}
