package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
public class PerMethodService {

    @Interceptors(PerMethodInterceptor.class)
    public String one() {
        return "one";
    }

    @Interceptors(PerMethodInterceptor.class)
    public String two() {
        return "two";
    }

    public String untouched() {
        return "untouched";
    }
}
