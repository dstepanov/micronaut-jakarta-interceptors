package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(OverridingInterceptor.class)
public class OverriddenService {

    public String work() {
        Calls.RECORDED.add("target");
        return "done";
    }
}
