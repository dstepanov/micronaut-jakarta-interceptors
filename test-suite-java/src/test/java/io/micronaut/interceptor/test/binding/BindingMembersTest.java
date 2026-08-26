package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BindingMembersTest {

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
    void bindsAnInterceptorToTheMembersItDeclares() {
        context.getBean(CachingService.class).withUsersRegion();

        assertEquals(List.of("users region"), Calls.RECORDED);
    }

    @Test
    void bindsThroughTheValueAMemberDefaultsTo() {
        context.getBean(CachingService.class).withDefaultRegion();

        assertEquals(List.of("default region"), Calls.RECORDED);
    }

    @Test
    void bindsTheDeclaredValueAndTheDefaultOneAlike() {
        context.getBean(CachingService.class).withDefaultRegionSpeltOut();

        assertEquals(List.of("default region"), Calls.RECORDED);
    }

    @Test
    void leavesAMethodWithoutTheBindingAlone() {
        assertEquals("none", context.getBean(CachingService.class).withoutCaching());
        assertEquals(List.of(), Calls.RECORDED);
    }
}
