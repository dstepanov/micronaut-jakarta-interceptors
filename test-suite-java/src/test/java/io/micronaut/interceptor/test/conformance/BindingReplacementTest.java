package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BindingReplacementTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void clear() {
        Calls.clear();
    }

    /** 3.3 db) A binding declared on a method replaces the binding of the same type declared at class level. */
    @Test
    void theBindingOfAMethodReplacesTheOneOfItsClass() {
        context.getBean(RegionService.class).fromTheMethod();

        assertEquals(List.of("region b"), Calls.RECORDED);
    }

    @Test
    void aMethodWithoutOneKeepsTheBindingOfItsClass() {
        context.getBean(RegionService.class).fromTheClass();

        assertEquals(List.of("region a"), Calls.RECORDED);
    }

    /** 2.5 ba) and bb) An interceptor may suppress an exception and recover by proceeding again. */
    @Test
    void suppressesAnExceptionAndRecoversByProceedingAgain() {
        assertEquals("recovered on attempt 2", context.getBean(FlakyService.class).work());

        assertEquals(List.of("suppressed first attempt"), Calls.RECORDED);
    }
}
