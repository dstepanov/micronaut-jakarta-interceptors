package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstructorInterceptorsTest {

    @Test
    void invokesAnInterceptorClassNamedByTheConstructor() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ConstructorNamedInterceptor.CALLS.clear();

            assertEquals("done", context.getBean(NamedOnConstructorService.class).work());
            assertEquals(List.of("named"), ConstructorNamedInterceptor.CALLS);
        }
    }

    /**
     * Section 4 ca) of the specification: an interceptor named by a constructor is invoked in addition to the
     * ones named by the class, the class's first. The other test names one on the constructor alone, where the
     * two lists cannot be told apart because one of them is empty.
     */
    @Test
    void invokesTheInterceptorOfTheClassAndTheOneOfTheConstructor() {
        try (ApplicationContext context = ApplicationContext.run()) {
            ConstructorNamedInterceptor.CALLS.clear();

            context.getBean(NamedOnBothService.class);

            assertEquals(List.of("class named", "named", "constructed"),
                List.copyOf(ConstructorNamedInterceptor.CALLS));
        }
    }
}