package io.micronaut.interceptor.test.shutdown;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.test.shutdown.early.AGuardingInterceptor;
import io.micronaut.interceptor.test.shutdown.late.ZDependingService;
import io.micronaut.interceptor.test.shutdown.late.ZGuardedService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3 cc): a pre-destroy interceptor method is invoked before the target instance and the interceptor instances
 * associated with it are destroyed. An interceptor class the application declares a {@code @Singleton} is a bean of
 * the context rather than of the object it intercepts, and the order a closing context destroys its singletons in is
 * Micronaut's to decide.
 *
 * <p>Micronaut derives that order from what a bean requires: its injection points, {@code @DependsOn}, and what was
 * resolved for it while it lived. The interceptor instances of an object are resolved as dependencies of its advice,
 * so a singleton interceptor is among what the object requires, and outlives it whichever way the names of the two
 * definitions sort.</p>
 */
class ShutdownOrderingTest {

    /**
     * Nothing the application declares orders the two, and the definition of the interceptor is the one that sorts
     * first. The interceptor still outlives the bean whose destruction it interposes on.
     */
    @Test
    void keepsASingletonInterceptorUntilTheBeanItInterceptsIsDestroyed() {
        AGuardingInterceptor.reset();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ZGuardedService.class).work());
        }

        assertEquals(List.of(
            "intercepted a destruction",
            "service destroyed",
            "interceptor closed"), AGuardingInterceptor.RECORDED);
    }

    /**
     * A bean that declares the interceptor class among the beans it depends on is destroyed first as well, and the
     * interception of its destruction runs on an interceptor that is still open.
     */
    @Test
    void keepsASingletonInterceptorUntilABeanThatDependsOnItIsDestroyed() {
        AGuardingInterceptor.reset();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ZDependingService.class).work());
        }

        assertEquals(List.of(
            "intercepted a destruction",
            "service destroyed",
            "interceptor closed"), AGuardingInterceptor.RECORDED);
    }
}
