package io.micronaut.interceptor.test.graalvm;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

@Singleton
@Guarded
public class GuardedService extends GuardedBase {

    @PostConstruct
    void init() {
        Calls.RECORDED.add("own");
    }

    public String work() {
        return "done";
    }
}
