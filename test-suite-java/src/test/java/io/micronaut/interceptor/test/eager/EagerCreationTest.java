package io.micronaut.interceptor.test.eager;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3: an interceptor instance is created when the object it intercepts is created, before anything is invoked on it,
 * for every shape of bean: one proxied as a subclass, one proxied with a separate target, and one a factory produces,
 * each bound on the class or on a method alone.
 */
class EagerCreationTest {

    @Test
    void methodOnlyPlain() {
        assertCreatedWithTheObject(MethodOnlyPlain.class, bean -> ((MethodOnlyPlain) bean).work());
    }

    @Test
    void methodOnlyProxyTarget() {
        assertCreatedWithTheObject(MethodOnlyProxyTarget.class, bean -> ((MethodOnlyProxyTarget) bean).work());
    }

    @Test
    void classLevelProxyTarget() {
        assertCreatedWithTheObject(ClassLevelProxyTarget.class, bean -> ((ClassLevelProxyTarget) bean).work());
    }

    @Test
    void methodOnlyProduced() {
        assertCreatedWithTheObject(MethodOnlyProduced.class, bean -> ((MethodOnlyProduced) bean).work());
    }

    @Test
    void classLevelProduced() {
        assertCreatedWithTheObject(ClassLevelProduced.class, bean -> ((ClassLevelProduced) bean).work());
    }

    private static void assertCreatedWithTheObject(Class<?> type, Function<Object, String> call) {
        EagerInterceptor.CREATED.clear();
        EagerInterceptor.INVOKED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            Object bean = context.getBean(type);
            int createdBeforeTheCall = EagerInterceptor.CREATED.size();
            assertEquals("done", call.apply(bean));
            assertEquals(1, createdBeforeTheCall, type + ": created before the first call");
            assertEquals(1, EagerInterceptor.INVOKED.size(), type + ": invoked");
            assertEquals(1, EagerInterceptor.CREATED.size(), type + ": instances " + EagerInterceptor.CREATED);
        }
    }
}
