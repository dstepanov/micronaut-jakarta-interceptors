package io.micronaut.interceptor.test.state;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Records which interceptor instance intercepted which object, so that a test can tell one from the other.
 */
public final class CountingInterceptors {

    private static final Map<Object, CountingInterceptor> BY_TARGET = new IdentityHashMap<>();

    private CountingInterceptors() {
    }

    static synchronized void seen(Object target, CountingInterceptor interceptor) {
        BY_TARGET.put(target, interceptor);
    }

    static synchronized int of(Object target) {
        CountingInterceptor interceptor = BY_TARGET.get(target);
        return interceptor == null ? -1 : interceptor.count();
    }
}
