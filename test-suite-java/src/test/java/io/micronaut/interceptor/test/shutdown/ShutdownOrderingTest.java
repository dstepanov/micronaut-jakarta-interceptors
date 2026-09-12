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
 * associated with it are destroyed. That holds for the interceptor instances this module owns, which it destroys
 * itself once the object they intercepted is gone. An interceptor class the application declares a {@code @Singleton}
 * is not one of them: it is a bean of the context, and the order a closing context destroys its singletons in is
 * Micronaut's to decide.
 *
 * <p>Micronaut derives that order from what the definition of a bean requires - its injection points and
 * {@code @DependsOn} - and a bean bound to an interceptor by an annotation requires nothing: the interceptor classes
 * of an element are resolved when it is intercepted, from the bean index, which the definition knows nothing of. The
 * two are then destroyed in the order of the names of their definitions, which is what these tests pin down, rather
 * than the order the specification asks for. The limitations section of the guide describes it.</p>
 */
class ShutdownOrderingTest {

    /**
     * The interceptor is closed before the bean whose destruction it interposes on, because nothing says otherwise
     * and its definition is the one that sorts first. The pre-destroy interception then runs on a closed instance,
     * which is the whole of the difference.
     */
    @Test
    void closesASingletonInterceptorBeforeTheBeanWhenNothingOrdersTheTwo() {
        AGuardingInterceptor.reset();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(ZGuardedService.class).work());
        }

        assertEquals(List.of(
            "interceptor closed",
            "intercepted a destruction after being closed",
            "service destroyed"), AGuardingInterceptor.RECORDED);
    }

    /**
     * A bean that declares the interceptor class among the beans it depends on is destroyed first, and the
     * interception of its destruction runs on an interceptor that is still open. That is what an application whose
     * singleton interceptor holds resources has to declare.
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
