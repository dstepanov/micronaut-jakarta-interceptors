package io.micronaut.interceptor.test.cdi;

import jakarta.inject.Singleton;

/** Overrides the bound method without declaring the binding again. */
@Singleton
public class OverridesGuardedService extends GuardedBase {

    @Override
    public void guarded() {
        Calls.RECORDED.add("overridden");
    }
}
