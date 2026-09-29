package io.micronaut.interceptor.test.eager;

import jakarta.inject.Singleton;

@Singleton
public class MethodOnlyPlain {
    @Eager
    public String work() { return "done"; }
}
