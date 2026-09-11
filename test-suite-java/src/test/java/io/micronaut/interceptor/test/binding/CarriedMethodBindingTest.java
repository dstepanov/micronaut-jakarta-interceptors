package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A binding annotation declared on another annotation binds through it, whatever that annotation is otherwise for,
 * on a method as much as on a class.
 */
class CarriedMethodBindingTest {

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
    void bindsTheClassAPlainAnnotationCarriesTheBindingTo() {
        assertEquals("work", context.getBean(CarriedBindingServices.SurveilledClass.class).work());

        assertEquals(List.of("watched work"), Calls.RECORDED);
    }

    @Test
    void bindsTheMethodAPlainAnnotationCarriesTheBindingTo() {
        CarriedBindingServices.SurveilledMethod bean = context.getBean(CarriedBindingServices.SurveilledMethod.class);
        assertEquals("work", bean.work());
        assertEquals("rest", bean.rest());

        assertEquals(List.of("watched work"), Calls.RECORDED);
    }

    @Test
    void bindsTheMethodAPlainAnnotationCarriesTheBindingToInAnInterceptedClass() {
        CarriedBindingServices.SurveilledBesideWatched bean =
            context.getBean(CarriedBindingServices.SurveilledBesideWatched.class);
        assertEquals("work", bean.work());
        assertEquals("direct", bean.direct());

        assertEquals(List.of("watched work", "watched direct"), Calls.RECORDED);
    }

    @Test
    void bindsTheMethodABindingAnnotationCarriesTheBindingTo() {
        assertEquals("work", context.getBean(CarriedBindingServices.GuardedMethod.class).work());

        assertEquals(List.of("watched work"), Calls.RECORDED);
    }
}
