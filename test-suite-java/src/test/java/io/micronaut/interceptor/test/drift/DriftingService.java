package io.micronaut.interceptor.test.drift;

import jakarta.inject.Singleton;

@Singleton
@Drifting
public class DriftingService {

    public String work() {
        return "work";
    }
}
