package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstructorBindingRulesTest {

    /**
     * 3.3 bc) A constructor may declare several bindings, and 3.3 da) one of them replaces the binding of the same
     * type declared at class level.
     */
    @Test
    void aConstructorMayDeclareSeveralBindingsAndReplaceTheOneOfItsClass() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.createBean(ZonedService.class);

            assertEquals(List.of("audited", "zone b"), Calls.RECORDED);
        }
    }

    /** 5.2.1 c) The Priority annotation is ignored on an interceptor bound with the Interceptors annotation. */
    @Test
    void ignoresThePriorityOfAnInterceptorNamedDirectly() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(NamedPriorityService.class).work();

            assertEquals(List.of("late priority", "early priority"), Calls.RECORDED,
                "the order they are named in wins over their priorities");
        }
    }
}
