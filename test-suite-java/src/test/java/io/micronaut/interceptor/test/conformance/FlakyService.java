package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
@Recovering
public class FlakyService {

    private int attempts;

    public String work() {
        attempts++;
        if (attempts == 1) {
            throw new IllegalStateException("first attempt");
        }
        return "recovered on attempt " + attempts;
    }
}
