package io.micronaut.interceptor.test.graalvm;

import jakarta.annotation.PostConstruct;

/** The superclass callback, which the specification invokes before the one of the class itself. */
public class GuardedBase {

    @PostConstruct
    void initBase() {
        Calls.RECORDED.add("base");
    }
}
