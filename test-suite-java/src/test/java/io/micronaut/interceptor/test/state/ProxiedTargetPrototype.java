package io.micronaut.interceptor.test.state;

import io.micronaut.aop.Around;
import io.micronaut.context.annotation.Prototype;

@Prototype
@Paired
@Around(proxyTarget = true)
public class ProxiedTargetPrototype {

    public String work() {
        return "done";
    }
}
