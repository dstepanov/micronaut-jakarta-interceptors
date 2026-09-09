package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.event.ApplicationEventPublisher;
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
    void theMethodsOfObjectAreNotBusinessMethodsEvenWhenOverridden() {
        try (ApplicationContext context = ApplicationContext.run()) {
            OverridingObjectService service = context.getBean(OverridingObjectService.class);

            Calls.clear();
            service.toString();
            service.hashCode();
            service.equals((Object) service);
            assertEquals(List.of("toString", "hashCode", "equals"), List.copyOf(Calls.RECORDED));

            // a method that only shares a name with one of them is an overload rather than an override, and is a
            // business method as any other method is
            Calls.clear();
            service.toString("a ");
            service.equals(service);
            service.work();
            assertEquals(
                List.of("secure", "toString overload", "secure", "equals overload", "secure", "work"),
                List.copyOf(Calls.RECORDED));
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

    /**
     * An observer method is a business method: the kit checks this of a CDI observer method, and the counterpart
     * here is a method a listener declares for an event the application publishes.
     */
    @Test
    void anObserverMethodIsIntercepted() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(SignalListener.class);
            Calls.clear();

            context.getBean(ApplicationEventPublisher.class).publishEvent(new Signal("fired"));

            assertEquals(List.of("monitored", "observed fired"), List.copyOf(Calls.RECORDED));
        }
    }
}