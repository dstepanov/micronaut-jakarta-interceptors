package io.micronaut.interceptor.test.introduction;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A bean Micronaut implements with introduction advice is intercepted as any other bean is. Micronaut tells such an
 * invocation apart as an introduction, and the specification has no kind of its own for it: an invocation of a
 * business method is one whether a class or an advice implements the method, so the interceptor classes interpose
 * on it, ahead of the advice that implements it.
 */
class IntroductionAdviceTest {

    @BeforeEach
    void reset() {
        Calls.RECORDED.clear();
    }

    @Test
    void aBoundInterceptorInterposesOnAnIntroducedInterfaceMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("introduced", context.getBean(IntroducedService.class).hello());

            assertEquals(List.of("bound hello", "introducer hello"), Calls.RECORDED);
        }
    }

    @Test
    void aNamedInterceptorInterposesOnAnIntroducedAbstractMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("introduced", context.getBean(PartlyIntroducedService.class).hello());

            assertEquals(List.of("named hello", "introducer hello"), Calls.RECORDED);
        }
    }

    @Test
    void aNamedInterceptorInterposesOnAConcreteMethodOfAnIntroducedClass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("concrete", context.getBean(PartlyIntroducedService.class).concrete());

            // Micronaut applies introduction advice to the abstract methods alone, so the concrete method is
            // proceeded into by the interceptor directly
            assertEquals(List.of("named concrete", "concrete"), Calls.RECORDED);
        }
    }
}
