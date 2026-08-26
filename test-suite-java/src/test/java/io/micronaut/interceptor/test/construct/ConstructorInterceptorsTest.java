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
}
