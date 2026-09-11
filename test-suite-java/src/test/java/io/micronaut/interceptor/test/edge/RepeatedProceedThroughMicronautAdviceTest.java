package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 2.5 bb) of the specification, where what the chain proceeds into begins with an ordinary Micronaut advice.
 *
 * <p>An interceptor that recovers by proceeding a second time runs everything after it again, and a Micronaut
 * interceptor ordered after the Jakarta Interceptors of the bean is part of that. Proceeding again must not drop
 * straight through to the intercepted method, leaving the Micronaut interceptor out of the second attempt.</p>
 */
class RepeatedProceedThroughMicronautAdviceTest {

    @Test
    void proceedingASecondTimeRunsTheLaterMicronautAdviceAgain() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Log.RECORDED.clear();

            assertEquals("succeeded", context.getBean(RetriedService.class).attempt());

            assertEquals(
                List.of("retry", "micronaut advice", "target", "retry again", "micronaut advice", "target"),
                Log.RECORDED);
        }
    }
}
