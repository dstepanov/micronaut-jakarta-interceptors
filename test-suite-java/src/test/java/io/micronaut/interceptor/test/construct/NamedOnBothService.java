package io.micronaut.interceptor.test.construct;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

/**
 * Names an interceptor on the class and another on the constructor: the specification invokes the one of the
 * constructor in addition to the one of the class, not instead of it.
 */
@Singleton
@Interceptors(ClassNamedConstructInterceptor.class)
public class NamedOnBothService {

    @Interceptors(ConstructorNamedInterceptor.class)
    public NamedOnBothService() {
        ConstructorNamedInterceptor.CALLS.add("constructed");
    }
}
