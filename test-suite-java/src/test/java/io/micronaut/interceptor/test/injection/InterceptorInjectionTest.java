package io.micronaut.interceptor.test.injection;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterceptorInjectionTest {

    @Test
    void injectsIntoAnInterceptorClass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("saved", context.getBean(AuditedService.class).save());
            assertEquals(List.of("save"), context.getBean(AuditLog.class).entries());
        }
    }
}
