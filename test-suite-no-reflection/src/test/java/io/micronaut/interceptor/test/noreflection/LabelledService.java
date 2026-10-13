package io.micronaut.interceptor.test.noreflection;

import jakarta.inject.Singleton;

@Singleton
@Labelled
public class LabelledService {

    public String work() {
        return "labelled";
    }
}
