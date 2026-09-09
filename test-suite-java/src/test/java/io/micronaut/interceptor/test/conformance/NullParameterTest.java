package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 2.4 g) of the specification: the arguments an interceptor sets must match the signature of the
 * intercepted method. What that means for null is the part no other test reaches - null is a value of a reference
 * argument and is not a value of a primitive one - and the array itself may not be null either.
 */
class NullParameterTest {

    @Test
    void nullIsAValueOfAReferenceArgumentAndNotOfAPrimitiveOne() {
        try (ApplicationContext context = ApplicationContext.run()) {
            NulledService service = context.getBean(NulledService.class);

            Calls.clear();
            assertEquals("reference null", service.reference("Denis"),
                "null replaced the argument and reached the method");
            assertEquals(
                List.of("null array -> IllegalArgumentException", "null value -> accepted"),
                List.copyOf(Calls.RECORDED));

            Calls.clear();
            assertEquals("primitive 1", service.primitive(1),
                "the primitive argument was left as it was");
            assertEquals(
                List.of("null array -> IllegalArgumentException", "null value -> IllegalArgumentException"),
                List.copyOf(Calls.RECORDED));
        }
    }
}
