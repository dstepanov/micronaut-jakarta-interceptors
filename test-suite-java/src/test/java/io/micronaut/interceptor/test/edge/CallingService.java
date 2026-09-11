package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;

/**
 * A bean whose one business method calls another on {@code this}, proxied the way Micronaut proxies a bean by
 * default: the proxy is a subclass, and it is the bean.
 */
@Singleton
@Relayed
public class CallingService {

    public String outer() {
        Log.RECORDED.add("outer body");
        return inner();
    }

    public String inner() {
        Log.RECORDED.add("inner body");
        return "inner";
    }
}
