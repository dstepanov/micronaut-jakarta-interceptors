package io.micronaut.interceptor.test.variations;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InheritedInterceptorMethodTest {

    @Test
    void anInterceptorMethodMayBeInheritedFromASuperclass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            InheritingService service = context.getBean(InheritingService.class);
            // the interceptor class also interposes on the construction of this bean, which the other test is
            // about; cleared here so that what is asserted is the interception of the business method alone
            Calls.clear();

            assertEquals("done", service.work());
            assertEquals(List.of("inherited interceptor method", "target"), Calls.RECORDED);
        }
    }

    /**
     * Section 2.7 i) of the specification: an interceptor method may be inherited, and the one a superclass
     * declares is invoked before the one the subclass declares. Asserted for construction as well as for a
     * business method, the two being separate methods of the interceptor class and separately inherited.
     */
    @Test
    void anInheritedConstructorInterceptorMethodRunsBeforeTheDeclaredOne() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.RECORDED.clear();

            context.getBean(InheritingService.class);

            assertEquals(List.of("inherited aroundConstruct", "declared aroundConstruct"),
                List.copyOf(Calls.RECORDED));
        }
    }
}