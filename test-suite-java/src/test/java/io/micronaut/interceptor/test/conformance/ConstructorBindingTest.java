package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 3.3 ac) and 3.4 cc) of the specification: an interceptor may be bound by annotating a constructor of the
 * component class.
 */
class ConstructorBindingTest {

    @Test
    void bindsAnInterceptorDeclaredOnTheConstructor() {
        Calls.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ConstructorBoundService.class);

            assertEquals(List.of("constructor bound interceptor", "constructor"), Calls.RECORDED);
        }
    }
}
