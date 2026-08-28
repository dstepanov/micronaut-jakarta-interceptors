package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
@PrivateLevels
public class PrivateLevelsService {

    public String work() {
        return "done";
    }
}
