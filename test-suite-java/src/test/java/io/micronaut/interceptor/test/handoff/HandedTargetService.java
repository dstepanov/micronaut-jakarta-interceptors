package io.micronaut.interceptor.test.handoff;

import io.micronaut.aop.Around;
import jakarta.inject.Singleton;

/** Proxied with a separate target, and watched by {@link MeddlingListener}. */
@Singleton
@Handed
@Around(proxyTarget = true)
public class HandedTargetService {

    public String work() {
        return "done";
    }
}
