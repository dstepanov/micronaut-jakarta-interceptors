package io.micronaut.interceptor.test.named;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterceptorsTest {

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

    @Test
    void invokesTheInterceptorsOfTheClassInTheOrderTheyAreNamed() {
        context.getBean(NamedService.class).inherited();

        assertEquals(List.of("Alpha", "Beta", "inherited"), Calls.RECORDED);
    }

    @Test
    void invokesTheInterceptorsOfTheMethodAfterTheOnesOfTheClass() {
        context.getBean(NamedService.class).withOwn();

        assertEquals(List.of("Alpha", "Beta", "Gamma", "withOwn"), Calls.RECORDED);
    }

    @Test
    void excludesTheInterceptorsOfTheClass() {
        context.getBean(NamedService.class).excluded();

        assertEquals(List.of("excluded"), Calls.RECORDED);
    }

    @Test
    void keepsTheInterceptorsOfTheMethodWhenTheOnesOfTheClassAreExcluded() {
        context.getBean(NamedService.class).excludedWithOwn();

        assertEquals(List.of("Gamma", "excludedWithOwn"), Calls.RECORDED);
    }
}
