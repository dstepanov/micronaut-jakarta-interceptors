package io.micronaut.interceptor.test.lifecycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LifecycleInterceptionTest {

    @Test
    void interposesOnThePostConstructCallback() {
        LifecycleInterceptor.CALLS.clear();
        LifecycleInterceptor.CALLBACKS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ManagedService.class);

            assertEquals(
                List.of("interceptor postConstruct, target is a ManagedService",
                    "target postConstruct",
                    "interceptor postConstruct done"),
                LifecycleInterceptor.CALLS);
        }
    }

    @Test
    void interposesOnThePreDestroyCallback() {
        LifecycleInterceptor.CALLS.clear();
        LifecycleInterceptor.CALLBACKS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ManagedService.class);
            LifecycleInterceptor.CALLS.clear();
        }
        assertEquals(List.of("interceptor preDestroy", "target preDestroy"), LifecycleInterceptor.CALLS);
    }

    /**
     * The specification shows a lifecycle callback interceptor method the callback of the class it is interposing
     * on. Micronaut interposes on the lifecycle of a bean rather than on one callback of it, so which method that
     * is was read from the class at compilation time.
     */
    @Test
    void showsTheInterceptorTheCallbackOfTheInterceptedClass() {
        LifecycleInterceptor.CALLS.clear();
        LifecycleInterceptor.CALLBACKS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ManagedService.class);

            assertEquals(1, LifecycleInterceptor.CALLBACKS.size());
            Method callback = LifecycleInterceptor.CALLBACKS.get(0);
            assertEquals("start", callback.getName());
            assertEquals(ManagedService.class, callback.getDeclaringClass());
        }
        assertEquals(2, LifecycleInterceptor.CALLBACKS.size());
        Method callback = LifecycleInterceptor.CALLBACKS.get(1);
        assertEquals("stop", callback.getName());
        assertEquals(ManagedService.class, callback.getDeclaringClass());
    }

    /**
     * There is nothing to show an interceptor when the intercepted class declares no callback, which is the one
     * case the specification has {@code getMethod()} return {@code null} for a lifecycle callback.
     */
    @Test
    void showsNoCallbackWhenTheInterceptedClassDeclaresNone() {
        LifecycleInterceptor.CALLS.clear();
        LifecycleInterceptor.CALLBACKS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(CallbacklessService.class).work());

            assertEquals(1, LifecycleInterceptor.CALLBACKS.size());
            assertNull(LifecycleInterceptor.CALLBACKS.get(0));
        }
    }
}
