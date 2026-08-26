package io.micronaut.interceptor.test.construct;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
public class NamedOnConstructorService {

    @Interceptors(ConstructorNamedInterceptor.class)
    public NamedOnConstructorService() {
    }

    public String work() {
        return "done";
    }
}
