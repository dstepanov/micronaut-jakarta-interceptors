package io.micronaut.interceptor.test.eager;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3: an interceptor instance is created when the object it intercepts is created, before anything is invoked on it,
 * for every shape of bean: one proxied as a subclass, one proxied with a separate target, and one a factory produces,
 * each bound on the class or on a method alone. A bean created through {@code BeanContext.createBean} is no
 * exception: Micronaut begins a creation for it as it does for a lookup, and hands its advice the bean all the same.
 */
class EagerCreationTest {

    @Test
    void methodOnlyPlain() {
        assertCreatedWithTheObject(MethodOnlyPlain.class, bean -> ((MethodOnlyPlain) bean).work());
    }

    @Test
    void methodOnlyPlainCreatedOnDemand() {
        assertCreatedOnDemandWithTheObject(MethodOnlyPlain.class, bean -> ((MethodOnlyPlain) bean).work());
    }

    @Test
    void methodOnlyProxyTarget() {
        assertCreatedWithTheObject(MethodOnlyProxyTarget.class, bean -> ((MethodOnlyProxyTarget) bean).work());
    }

    @Test
    void methodOnlyProxyTargetCreatedOnDemand() {
        assertCreatedOnDemandWithTheObject(MethodOnlyProxyTarget.class, bean -> ((MethodOnlyProxyTarget) bean).work());
    }

    @Test
    void classLevelProxyTarget() {
        assertCreatedWithTheObject(ClassLevelProxyTarget.class, bean -> ((ClassLevelProxyTarget) bean).work());
    }

    @Test
    void classLevelProxyTargetCreatedOnDemand() {
        assertCreatedOnDemandWithTheObject(ClassLevelProxyTarget.class, bean -> ((ClassLevelProxyTarget) bean).work());
    }

    @Test
    void methodOnlyProduced() {
        assertCreatedWithTheObject(MethodOnlyProduced.class, bean -> ((MethodOnlyProduced) bean).work());
    }

    @Test
    void methodOnlyProducedCreatedOnDemand() {
        assertCreatedOnDemandWithTheObject(MethodOnlyProduced.class, bean -> ((MethodOnlyProduced) bean).work());
    }

    @Test
    void classLevelProduced() {
        assertCreatedWithTheObject(ClassLevelProduced.class, bean -> ((ClassLevelProduced) bean).work());
    }

    @Test
    void classLevelProducedCreatedOnDemand() {
        assertCreatedOnDemandWithTheObject(ClassLevelProduced.class, bean -> ((ClassLevelProduced) bean).work());
    }

    private static void assertCreatedWithTheObject(Class<?> type, Function<Object, String> call) {
        assertCreatedWithTheObject(type, call, false);
    }

    private static void assertCreatedOnDemandWithTheObject(Class<?> type, Function<Object, String> call) {
        assertCreatedWithTheObject(type, call, true);
    }

    private static void assertCreatedWithTheObject(Class<?> type, Function<Object, String> call, boolean onDemand) {
        EagerInterceptor.CREATED.clear();
        EagerInterceptor.INVOKED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            Object bean = onDemand ? context.createBean(type) : context.getBean(type);
            int createdBeforeTheCall = EagerInterceptor.CREATED.size();
            assertEquals("done", call.apply(bean));
            assertEquals(1, createdBeforeTheCall, type + ": created before the first call");
            assertEquals(1, EagerInterceptor.INVOKED.size(), type + ": invoked");
            assertEquals(1, EagerInterceptor.CREATED.size(), type + ": instances " + EagerInterceptor.CREATED);
        }
    }
}
