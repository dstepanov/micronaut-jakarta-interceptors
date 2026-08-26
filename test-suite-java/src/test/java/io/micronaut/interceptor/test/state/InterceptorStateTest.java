package io.micronaut.interceptor.test.state;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterceptorStateTest {

    /**
     * The specification associates an interceptor instance with the object it intercepts, which is what lets an
     * interceptor hold state for the whole life of that object without seeing the invocations of another.
     */
    @Test
    void anInterceptorInstanceBelongsToOneInterceptedObject() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CountedService first = context.getBean(CountedService.class);
            CountedService second = context.getBean(CountedService.class);

            first.work();
            first.work();
            second.work();

            assertEquals(2, countOf(first));
            assertEquals(1, countOf(second));
        }
    }

    private static int countOf(CountedService service) {
        // the interceptor counts what it saw; the count is read back through an invocation of its own
        return CountingInterceptors.of(service);
    }
}
