package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a method declares is declared by that method: an override of it that does not declare it again does not have
 * it. That holds for the bindings of the overridden method where the overriding class is bound itself - the bindings
 * of the class are then the only ones of the override - and for the interceptor classes the overridden method names,
 * and for its exclusion of the interceptors of the class.
 */
class OverriddenMethodBindingsTest {

    /** Bound at the class, and overriding a method that declares a binding of its own. */
    @Singleton
    @Monitored
    public static class BoundOverridingService extends GuardedBase {
        @Override
        public void guarded() {
            Calls.RECORDED.add("overridden");
        }
    }

    public static class NamedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            Calls.RECORDED.add("named");
            return context.proceed();
        }
    }

    public static class NamingBase {
        @Interceptors(NamedInterceptor.class)
        public void named() {
            Calls.RECORDED.add("base");
        }

        @ExcludeClassInterceptors
        public void excluding() {
            Calls.RECORDED.add("base");
        }
    }

    @Singleton
    @Monitored
    public static class NamingOverridingService extends NamingBase {
        @Override
        public void named() {
            Calls.RECORDED.add("overridden");
        }
    }

    public static class ClassLevelInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            Calls.RECORDED.add("class level");
            return context.proceed();
        }
    }

    @Singleton
    @Interceptors(ClassLevelInterceptor.class)
    public static class ExclusionOverridingService extends NamingBase {
        @Override
        public void excluding() {
            Calls.RECORDED.add("overridden");
        }
    }

    @Test
    void theBindingOfAnOverriddenMethodDoesNotComeBackWhereTheClassIsBound() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(BoundOverridingService.class).guarded();

            assertEquals(List.of("monitored", "overridden"), List.copyOf(Calls.RECORDED));
        }
    }

    @Test
    void theInterceptorClassesOfAnOverriddenMethodAreNotInherited() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(NamingOverridingService.class).named();

            assertEquals(List.of("monitored", "overridden"), List.copyOf(Calls.RECORDED));
        }
    }

    @Test
    void theExclusionOfAnOverriddenMethodIsNotInherited() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(ExclusionOverridingService.class).excluding();

            assertEquals(List.of("class level", "overridden"), List.copyOf(Calls.RECORDED));
        }
    }
}
