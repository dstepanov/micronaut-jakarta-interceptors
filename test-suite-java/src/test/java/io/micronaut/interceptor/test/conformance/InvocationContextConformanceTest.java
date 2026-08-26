package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocationContextConformanceTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void clear() {
        Calls.clear();
    }

    private static String only(String prefix) {
        return Calls.RECORDED.stream().filter(it -> it.startsWith(prefix)).findFirst().orElseThrow();
    }

    /** 2.4 ba) The same InvocationContext instance is passed to each interceptor method of one interception. */
    @Test
    void passesTheSameContextToEveryInterceptorOfOneInvocation() {
        context.getBean(CtxService.class).echo("a");

        assertEquals(only("first context ").substring("first context ".length()),
            only("second context ").substring("second context ".length()));
    }

    /** 2.4 ba) The context data is not shared across separate invocations. */
    @Test
    void doesNotShareTheContextDataAcrossInvocations() {
        context.getBean(CtxService.class).echo("a");
        assertEquals("first sees null", only("first sees "));

        Calls.clear();
        context.getBean(CtxService.class).echo("b");
        assertEquals("first sees null", only("first sees "),
            "the context data of the previous invocation must not leak into this one");
    }

    /** 2.3.1 b) Around-invoke interceptor methods run in the same thread as the target method. */
    @Test
    void runsInTheSameThreadAsTheTargetMethod() {
        context.getBean(CtxService.class).echo("a");

        String expected = Thread.currentThread().getName();
        assertEquals("first thread " + expected, only("first thread "));
        assertEquals("second thread " + expected, only("second thread "));
        assertEquals("target thread " + expected, only("target thread "));
    }

    /** 2.4 f) getParameters returns the values setParameters was called with. */
    @Test
    void readsBackTheParametersThatWereSet() {
        assertEquals("replaced", context.getBean(CtxService.class).echo("original"));
        assertEquals("after set replaced", only("after set "));
    }

    /** 2.4 gb) and gc) The number and the types of the parameters are checked. */
    @Test
    void rejectsParametersOfTheWrongCountOrType() {
        context.getBean(CtxService.class).echo("a");

        assertTrue(Calls.RECORDED.contains("wrong count rejected"), Calls.RECORDED.toString());
        assertTrue(Calls.RECORDED.contains("wrong type rejected"), Calls.RECORDED.toString());
    }

    /** 2.4 n) Every binding annotation is returned, including one that associates no interceptor. */
    @Test
    void returnsEveryBindingIncludingOnesThatBindNoInterceptor() {
        context.getBean(CtxService.class).echo("a");

        assertEquals("bindings [Ctx, Unbound]", only("bindings "));
    }

    /** 2.4 o) getInterceptorBinding returns the single annotation of the given type. */
    @Test
    void returnsASingleBindingByItsType() {
        context.getBean(CtxService.class).echo("a");

        assertEquals("unbound label declared", only("unbound label "));
    }

    /** 2.4 ba) The context data written by one interceptor reaches the ones that come after it. */
    @Test
    void sharesTheContextDataDownTheChain() {
        context.getBean(CtxService.class).echo("a");

        assertEquals("second sees written by the first", only("second sees "));
    }

}
