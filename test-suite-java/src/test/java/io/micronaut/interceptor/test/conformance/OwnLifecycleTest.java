package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnLifecycleTest {

    /**
     * 2.3 ca): a {@code @PostConstruct} method of an interceptor class that accepts an {@code InvocationContext}
     * interposes on the bean it intercepts; it is not the callback of the interceptor instance itself, and is not
     * invoked when that instance is created.
     */
    @Test
    void doesNotInvokeALifecycleInterceptorMethodAsTheInterceptorsOwnCallback() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(AccessLevelInterceptor.class);

            assertEquals(List.of(), Calls.RECORDED,
                "creating the interceptor must not run the callback it interposes with");
        }
    }

    /** 4 e) An interceptor named directly is invoked whether or not the component declares its binding. */
    @Test
    void ignoresTheBindingOfAnInterceptorThatIsNamedDirectly() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(NamesABoundInterceptorService.class).work());

            assertEquals(List.of("bound and named"), Calls.RECORDED);
        }
    }

    /** 2.8 bb) An around-timeout method may be declared on the target class. */
    @Test
    void invokesAnAroundTimeoutMethodDeclaredOnTheTargetClass() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(SelfTimeoutService.RAN.await(5, TimeUnit.SECONDS), "the schedule did not run");
            Thread.sleep(100);

            assertTrue(SelfTimeoutService.intercepted,
                "the around-timeout method of the target class interposes on its scheduled method");
        }
    }
}
