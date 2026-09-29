package io.micronaut.interceptor.test.named;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A class named by {@code @Interceptors} that does not declare {@code @Interceptor}, and whose only interceptor
 * methods interpose on lifecycle callbacks, is made a bean and interposes on the callbacks of the class naming it.
 */
class UndeclaredLifecycleOnlyInterceptorTest {

    @BeforeEach
    void clear() {
        Calls.clear();
    }

    @Test
    void interposesOnTheCallbacksOfTheClassNamingIt() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("done", context.getBean(UndeclaredLifecycleOnlyNamedService.class).work());

            assertEquals(List.of("undeclared postConstruct of the service", "target postConstruct"), Calls.RECORDED);
            Calls.clear();
        }
        assertEquals(List.of("undeclared preDestroy of the service", "target preDestroy"), Calls.RECORDED);
    }
}
