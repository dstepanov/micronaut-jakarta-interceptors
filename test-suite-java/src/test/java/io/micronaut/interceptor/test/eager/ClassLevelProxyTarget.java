package io.micronaut.interceptor.test.eager;

import io.micronaut.aop.Around;
import jakarta.inject.Singleton;

@Singleton
@Eager
@Around(proxyTarget = true)
public class ClassLevelProxyTarget {
    public String work() { return "done"; }
}
