package io.micronaut.interceptor.test.errors;

import io.micronaut.interceptor.test.external.UnprocessedBeanInterceptor;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(UnprocessedBeanInterceptor.class)
public class NamesUnprocessedBeanInterceptor {

    public String work() {
        return "done";
    }
}
