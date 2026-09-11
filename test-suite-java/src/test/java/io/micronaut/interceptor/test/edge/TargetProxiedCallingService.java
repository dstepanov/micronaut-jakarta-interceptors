package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;

/**
 * The same calls as {@link CallingService}, in a bean whose proxy wraps a separate target instance.
 */
@Singleton
@Relayed
@ProxiedTarget
public class TargetProxiedCallingService {

    public String outer() {
        Log.RECORDED.add("outer body");
        return inner();
    }

    public String inner() {
        Log.RECORDED.add("inner body");
        return "inner";
    }
}
