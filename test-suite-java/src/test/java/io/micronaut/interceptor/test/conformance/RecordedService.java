package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
@Recorded
public class RecordedService {

    public void voidMethod() {
    }

    public String work() {
        return "done";
    }
}
