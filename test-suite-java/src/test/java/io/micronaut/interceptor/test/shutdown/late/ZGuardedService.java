package io.micronaut.interceptor.test.shutdown.late;

import io.micronaut.interceptor.test.shutdown.early.AGuardingInterceptor;
import io.micronaut.interceptor.test.shutdown.early.Guarded;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

/**
 * A singleton bound to the interceptor by an annotation alone: it holds no reference to the interceptor, so nothing
 * of its definition says that the interceptor is still needed while this bean is destroyed.
 */
@Singleton
@Guarded
public class ZGuardedService {

    public String work() {
        return "done";
    }

    @PreDestroy
    void close() {
        AGuardingInterceptor.RECORDED.add("service destroyed");
    }
}
