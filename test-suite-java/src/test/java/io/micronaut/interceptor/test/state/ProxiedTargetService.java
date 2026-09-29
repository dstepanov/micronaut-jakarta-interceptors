package io.micronaut.interceptor.test.state;

import io.micronaut.aop.Around;
import jakarta.inject.Singleton;

/** Proxied with a separate target, so the business methods and the lifecycle are advised on two objects. */
@Singleton
@Paired
@Around(proxyTarget = true)
public class ProxiedTargetService {

    public String work() {
        return "done";
    }
}
