package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(NoopInterceptor.class)
public class SubTarget extends BaseTarget {

    public String work() {
        Calls.RECORDED.add("target");
        return "done";
    }
}
