package io.micronaut.interceptor.test.graalvm;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Traced("service")
public class TracedService {

    @Interceptors(NamedInterceptor.class)
    public String greet(String name) {
        return "Hello " + name;
    }
}
