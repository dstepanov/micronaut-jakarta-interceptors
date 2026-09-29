package io.micronaut.interceptor.test.eager;

import io.micronaut.aop.Around;
import jakarta.inject.Singleton;

@Singleton
@Around(proxyTarget = true)
public class MethodOnlyProxyTarget {
    @Eager
    public String work() { return "done"; }
}
