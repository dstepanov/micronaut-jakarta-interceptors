package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The typed binding accessors Jakarta Interceptors 2.2 added beside {@code getInterceptorBindings()}. The
 * specification gives them a default that filters the whole set; this module answers them without building the
 * rest, so what they return has to be held to the same answers.
 */
class TypedBindingTest {

    @Test
    void readsOneBindingAndARepeatedOneByType() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.RECORDED.clear();

            assertEquals("done", context.getBean(TypedBindingService.class).work());

            // UsersRegionInterceptor is bound by the @Cached(region = "users") the element also carries, and
            // runs after this one, the two sharing a priority and being ordered by their class name
            assertEquals(
                List.of("cached=users",
                        "labelled=[one, two]",
                        "tagged=null",
                        "taggedAll=[]",
                        "full=[Cached, Labelled(one), Labelled(two), TypedRead]",
                        "users region"),
                List.copyOf(Calls.RECORDED));
        }
    }
}
