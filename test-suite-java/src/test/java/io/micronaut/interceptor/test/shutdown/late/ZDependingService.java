package io.micronaut.interceptor.test.shutdown.late;

import io.micronaut.context.annotation.DependsOn;
import io.micronaut.interceptor.test.shutdown.early.AGuardingInterceptor;
import io.micronaut.interceptor.test.shutdown.early.Guarded;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

/**
 * The same bean, saying what the interception alone does not say: that the interceptor is a bean it needs, so the
 * closing context destroys it after this bean. {@code @DependsOn} is what an application has to reach for when a
 * singleton interceptor closes resources its pre-destroy interceptor method uses.
 */
@Singleton
@Guarded
@DependsOn(AGuardingInterceptor.class)
public class ZDependingService {

    public String work() {
        return "done";
    }

    @PreDestroy
    void close() {
        AGuardingInterceptor.RECORDED.add("service destroyed");
    }
}
