package io.micronaut.interceptor.test.lifecycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleInterceptionTest {

    @Test
    void interposesOnThePostConstructCallback() {
        LifecycleInterceptor.CALLS.clear();
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
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ManagedService.class);
            LifecycleInterceptor.CALLS.clear();
        }
        assertEquals(List.of("interceptor preDestroy", "target preDestroy"), LifecycleInterceptor.CALLS);
    }
}
