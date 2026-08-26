package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
@Managed
public class ManagedService {

    @PostConstruct
    void start() {
        LifecycleInterceptor.CALLS.add("target postConstruct");
    }

    @PreDestroy
    void stop() {
        LifecycleInterceptor.CALLS.add("target preDestroy");
    }
}
