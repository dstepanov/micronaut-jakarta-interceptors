package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
@Levels
public class LevelsService {

    public String work() {
        return "done";
    }
}
