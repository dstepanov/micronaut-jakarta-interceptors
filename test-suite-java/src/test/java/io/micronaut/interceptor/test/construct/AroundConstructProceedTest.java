
package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AroundConstructProceedTest {

    /** 2.3: the constructor runs once, however many times an interceptor proceeds. */
    @Test
    void proceedingTwiceConstructsOnce() {
        InsistentService.CONSTRUCTIONS.set(0);
        try (ApplicationContext context = ApplicationContext.run()) {
            context.createBean(InsistentService.class);
            assertEquals(1, InsistentService.CONSTRUCTIONS.get());
        }
    }
}
