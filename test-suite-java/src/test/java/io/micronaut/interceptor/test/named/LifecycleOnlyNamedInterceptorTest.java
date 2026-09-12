package io.micronaut.interceptor.test.named;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A class named by {@code @Interceptors} whose only interceptor methods interpose on lifecycle callbacks interposes
 * on them, whatever the compilation made of it first.
 */
class LifecycleOnlyNamedInterceptorTest {

    @BeforeEach
    void clear() {
        Calls.clear();
    }

    @Test
    void interposesOnTheCallbacksOfTheClassNamingIt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(LifecycleOnlyNamedService.class);

            assertEquals(List.of("lifecycle only postConstruct", "target postConstruct"), Calls.RECORDED);
            Calls.clear();
        }
        assertEquals(List.of("lifecycle only preDestroy", "target preDestroy"), Calls.RECORDED);
    }
}
