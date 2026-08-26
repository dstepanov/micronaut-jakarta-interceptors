package io.micronaut.interceptor.test.lifecycle;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Disabled;
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
        assertEquals(List.of("interceptor preDestroy"), LifecycleInterceptor.CALLS);
    }

    @Test
    @Disabled("Micronaut does not invoke the @PreDestroy method of a bean that has pre-destroy interception: the "
        + "generated doDispose of the intercepted bean definition calls preDestroy(..) but not the callback itself, "
        + "unlike doInitialize, which does call the @PostConstruct method")
    void proceedsIntoTheCallbackOfTheInterceptedBean() {
        LifecycleInterceptor.CALLS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ManagedService.class);
            LifecycleInterceptor.CALLS.clear();
        }
        assertEquals(List.of("interceptor preDestroy", "target preDestroy"), LifecycleInterceptor.CALLS);
    }
}
