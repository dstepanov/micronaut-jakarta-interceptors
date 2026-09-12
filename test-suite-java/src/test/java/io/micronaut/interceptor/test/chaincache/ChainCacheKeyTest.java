package io.micronaut.interceptor.test.chaincache;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A chain is remembered by what it is built from, and two elements built from different things must not be handed
 * one another's chain however alike what they declare hashes.
 *
 * <p>{@link Abmkvgcz} explains how these two beans come to declare interceptions that Micronaut's
 * {@code AnnotationValue} hashes alike and compares equal while naming different interceptor methods.</p>
 */
class ChainCacheKeyTest {

    @Test
    void doesNotHandTheBoundBeanTheChainOfTheSelfInterceptingBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.RECORDED.clear();
            assertEquals("collides", context.getBean(Abmkvgcz.class).work());
            assertEquals(List.of("self"), Calls.RECORDED);

            Calls.RECORDED.clear();
            assertEquals("marked", context.getBean(MarkedService.class).work());
            assertEquals(List.of("bound interceptor"), Calls.RECORDED);
        }
    }

    @Test
    void doesNotHandTheSelfInterceptingBeanTheChainOfTheBoundBean() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.RECORDED.clear();
            assertEquals("marked", context.getBean(MarkedService.class).work());
            assertEquals(List.of("bound interceptor"), Calls.RECORDED);

            Calls.RECORDED.clear();
            assertEquals("collides", context.getBean(Abmkvgcz.class).work());
            assertEquals(List.of("self"), Calls.RECORDED);
        }
    }
}
