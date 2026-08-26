package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
// named in an order that is the opposite of what their priorities would give
@Interceptors({LatePriorityInterceptor.class, EarlyPriorityInterceptor.class})
public class NamedPriorityService {

    public String work() {
        return "done";
    }
}
