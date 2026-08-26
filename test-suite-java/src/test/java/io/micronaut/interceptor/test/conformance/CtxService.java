package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;

@Singleton
@Ctx
@Unbound(label = "declared")
public class CtxService {

    public String echo(String value) {
        Calls.RECORDED.add("target thread " + Thread.currentThread().getName());
        return value;
    }
}
