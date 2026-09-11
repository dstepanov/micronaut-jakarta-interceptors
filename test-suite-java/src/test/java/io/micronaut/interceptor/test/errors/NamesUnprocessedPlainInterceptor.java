package io.micronaut.interceptor.test.errors;

import io.micronaut.interceptor.test.external.UnprocessedPlainInterceptor;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(UnprocessedPlainInterceptor.class)
public class NamesUnprocessedPlainInterceptor {

    public String work() {
        return "done";
    }
}
