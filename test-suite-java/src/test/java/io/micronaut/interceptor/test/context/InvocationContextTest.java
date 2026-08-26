package io.micronaut.interceptor.test.context;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocationContextTest {

    private static String result;

    @BeforeAll
    static void invokeOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            result = context.getBean(ObservedService.class).greet("denis");
        }
    }

    @Test
    void anInterceptorMayReplaceTheArgumentsAndTheResult() {
        assertEquals("Hello DENIS!", result);
    }

    @Test
    void theTargetIsTheInterceptedInstance() {
        assertInstanceOf(ObservedService.class, Observation.target);
    }

    @Test
    void theMethodIsTheInterceptedMethod() {
        assertEquals("greet", Observation.method.getName());
        assertEquals(1, Observation.method.getParameterCount());
    }

    @Test
    void thereIsNoConstructorAndNoTimerOnAMethodInvocation() {
        assertNull(Observation.constructor);
        assertNull(Observation.timer);
    }

    @Test
    void theParametersAreTheOnesTheCallerPassed() {
        assertEquals(List.of("denis"), Observation.parameters);
    }

    @Test
    void theContextDataIsSharedByTheWholeChain() {
        assertEquals(List.of("hello again"), Observation.contextData);
    }

    @Test
    void theBindingsAreTheOnesInEffect() {
        assertEquals(1, Observation.bindings.size());
        Observed binding = (Observed) Observation.bindings.iterator().next();
        assertEquals("service", binding.label());
        assertTrue(Observation.bindings.iterator().next() instanceof Observed);
    }
}
