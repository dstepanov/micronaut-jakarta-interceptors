package io.micronaut.interceptor.test.cdi;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Bound as a class, with an initializer method among its methods. */
@Singleton
@Secure
public class InitializedService {

    private Reporter reporter;

    @Inject
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
        Calls.RECORDED.add("initializer");
    }

    public String work() {
        Calls.RECORDED.add("work");
        return reporter == null ? "none" : "injected";
    }
}
