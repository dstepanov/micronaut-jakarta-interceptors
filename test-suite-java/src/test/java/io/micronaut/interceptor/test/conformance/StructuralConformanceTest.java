package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StructuralConformanceTest {

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

    /** 2.2 ac) An interceptor method may be defined in a superclass of the target class. */
    @Test
    void invokesAnInterceptorMethodDeclaredOnASuperclassOfTheTarget() {
        assertEquals("done", context.getBean(SubTarget.class).work());

        assertEquals(List.of("interceptor class", "superclass interceptor method", "target"), Calls.RECORDED);
    }

    /** 5.2 j) An interceptor method that is overridden is not invoked. */
    @Test
    void doesNotInvokeAnOverriddenInterceptorMethod() {
        assertEquals("done", context.getBean(OverriddenService.class).work());

        assertFalse(Calls.RECORDED.contains("must not be invoked"), Calls.RECORDED.toString());
    }

    /** 2.9 b) Only the interceptor methods relevant to what the interceptor is bound to are invoked. */
    @Test
    void invokesOnlyTheInterceptorMethodsOfTheBoundKind() {
        context.getBean(PerMethodService.class).one();

        assertFalse(Calls.RECORDED.stream().anyMatch(it -> it.startsWith("MUST NOT")), Calls.RECORDED.toString());
    }

    /** 2.9 d) and e) The same interceptor may serve several methods, through one instance per target instance. */
    @Test
    void keepsOneInterceptorInstanceForSeveralMethodsOfOneTarget() {
        PerMethodService service = context.getBean(PerMethodService.class);
        service.one();
        service.two();
        assertEquals("untouched", service.untouched());

        List<String> seen = Calls.RECORDED.stream().filter(it -> it.startsWith("method interceptor")).toList();
        assertEquals(2, seen.size(), seen.toString());
        assertEquals(instanceOf(seen.get(0)), instanceOf(seen.get(1)), "one interceptor instance: " + seen);
        assertEquals("1", countOf(seen.get(0)));
        assertEquals("2", countOf(seen.get(1)));
    }

    private static String instanceOf(String call) {
        return call.substring(call.indexOf("instance=") + "instance=".length());
    }

    private static String countOf(String call) {
        return call.substring(call.indexOf("seen=") + "seen=".length(), call.indexOf(" instance="));
    }
}
