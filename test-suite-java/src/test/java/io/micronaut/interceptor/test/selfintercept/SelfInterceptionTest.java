package io.micronaut.interceptor.test.selfintercept;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelfInterceptionTest {

    @Test
    void invokesTheInterceptorMethodOfTheClassAfterTheInterceptorClasses() {
        try (ApplicationContext context = ApplicationContext.run()) {
            SelfInterceptingService.CALLS.clear();

            assertEquals("done", context.getBean(SelfInterceptingService.class).work());
            assertEquals(
                List.of("interceptor class in", "self in", "target", "self out", "interceptor class out"),
                SelfInterceptingService.CALLS);
        }
    }
}
