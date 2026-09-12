package io.micronaut.interceptor.test.named;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptors;

@Singleton
@Interceptors(LifecycleOnlyInterceptor.class)
public class LifecycleOnlyNamedService {

    @PostConstruct
    void start() {
        Calls.RECORDED.add("target postConstruct");
    }

    @PreDestroy
    void stop() {
        Calls.RECORDED.add("target preDestroy");
    }
}
