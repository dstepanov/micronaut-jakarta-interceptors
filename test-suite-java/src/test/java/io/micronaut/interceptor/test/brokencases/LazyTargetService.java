package io.micronaut.interceptor.test.brokencases;

import io.micronaut.aop.Around;
import io.micronaut.interceptor.test.state.Paired;
import jakarta.inject.Singleton;

@Singleton
@Paired
@Around(proxyTarget = true, lazy = true, lazyInterceptorsPerTarget = true)
public class LazyTargetService {
    public String work() {
        return "done";
    }
}
