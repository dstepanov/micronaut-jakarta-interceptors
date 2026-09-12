
package io.micronaut.interceptor.test.introduction;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A method Micronaut introduces is a business method of the bean as far as the specification is concerned: an
 * interceptor bound to the bean interposes on it as on any other.
 */
class IntroductionInterceptionTest {

    @Test
    void interposesOnAnIntroducedMethod() {
        TracingInterceptor.CALLS.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("42", context.getBean(Oracle.class).answer());
            assertEquals(List.of("answer"), TracingInterceptor.CALLS);
        }
    }
}
