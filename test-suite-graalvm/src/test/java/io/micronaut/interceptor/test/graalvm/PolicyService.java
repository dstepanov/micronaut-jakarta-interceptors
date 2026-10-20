package io.micronaut.interceptor.test.graalvm;

import jakarta.inject.Singleton;

@Singleton
@Policy(detail = @Detail(value = "service", note = @Note("noted")), details = {@Detail("first"), @Detail("second")})
public class PolicyService {
    public String work() {
        return "done";
    }
}
