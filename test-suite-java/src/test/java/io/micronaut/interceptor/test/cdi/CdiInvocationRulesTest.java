package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdiInvocationRulesTest {

    @Test
    void aMethodLevelBindingIsInheritedByASubclassThatDoesNotOverrideTheMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(InheritsGuardedService.class).guarded();

            assertEquals(List.of("secure", "guarded"), List.copyOf(Calls.RECORDED));
        }
    }

    @Test
    void aMethodLevelBindingIsNotInheritedByAnOverride() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(OverridesGuardedService.class).guarded();

            assertEquals(List.of("overridden"), List.copyOf(Calls.RECORDED));
        }
    }

    @Test
    void anInitializerMethodIsNotABusinessMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            InitializedService service = context.getBean(InitializedService.class);
            assertEquals(List.of("initializer"), List.copyOf(Calls.RECORDED));

            Calls.clear();
            assertEquals("injected", service.work());
            assertEquals(List.of("secure", "work"), List.copyOf(Calls.RECORDED));
        }
    }
}
