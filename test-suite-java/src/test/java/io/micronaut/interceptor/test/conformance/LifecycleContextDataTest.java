package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Section 2.4 ba) of the specification, for a lifecycle event: the same {@code InvocationContext} is given to
 * every interceptor method of one interception, and the context data is not shared with another.
 *
 * <p>{@code InvocationContextConformanceTest} holds an {@code @AroundInvoke} invocation to the same rule. A
 * lifecycle event is a separate context of its own, built where the advice interposes on an event rather than on
 * a method, so what is true of one says nothing about the other.</p>
 */
class LifecycleContextDataTest {

    private static String only(String prefix) {
        return Calls.RECORDED.stream()
            .filter(it -> it.startsWith(prefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("nothing recorded for [" + prefix + "]: " + Calls.RECORDED));
    }

    /** The identity of the context an interceptor was given, which is what was recorded after the prefix. */
    private static String contextOf(String interceptor, String event) {
        String prefix = interceptor + " " + event + " context ";
        return only(prefix).substring(prefix.length());
    }

    @Test
    void passesTheSameContextToEveryInterceptorOfOneEvent() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            LifecycleCtxService bean = context.createBean(LifecycleCtxService.class);

            assertEquals(contextOf("first", "postConstruct"), contextOf("second", "postConstruct"),
                "one chain runs for the event, so both interceptor methods are given the same context");
            assertEquals("second postConstruct sees written by the first",
                only("second postConstruct sees "),
                "what the first wrote is there for the second");

            Calls.clear();
            context.destroyBean(bean);

            assertEquals(contextOf("first", "preDestroy"), contextOf("second", "preDestroy"),
                "and the same of a pre-destroy event");
            assertEquals("second preDestroy sees written by the first", only("second preDestroy sees "));
        }
    }

    @Test
    void doesNotShareTheContextDataAcrossEventsOrBeans() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            LifecycleCtxService first = context.createBean(LifecycleCtxService.class);
            String constructionContext = contextOf("first", "postConstruct");
            assertEquals("first postConstruct sees null", only("first postConstruct sees "));

            Calls.clear();
            context.createBean(LifecycleCtxService.class);
            assertEquals("first postConstruct sees null", only("first postConstruct sees "),
                "the data of one bean's event must not reach another bean's");
            assertNotEquals(constructionContext, contextOf("first", "postConstruct"),
                "nor is it the same context");

            Calls.clear();
            context.destroyBean(first);
            assertEquals("first preDestroy sees null", only("first preDestroy sees "),
                "nor does the data of the construction of a bean reach its destruction");
        }
    }
}
