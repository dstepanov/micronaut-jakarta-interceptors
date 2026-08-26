package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(BoundAndNamedInterceptor.class)
public class NamesABoundInterceptorService {

    public String work() {
        return "done";
    }
}
