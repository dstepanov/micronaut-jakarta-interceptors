package io.micronaut.interceptor.test.defaults;

import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(FactoryMadeInterceptor.class)
public class FactoryUsingBean {

    public String work() {
        return "factoryUsing";
    }
}
