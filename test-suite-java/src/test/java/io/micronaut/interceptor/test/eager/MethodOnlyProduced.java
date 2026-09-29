package io.micronaut.interceptor.test.eager;

public class MethodOnlyProduced {
    @Eager
    public String work() { return "done"; }
}
