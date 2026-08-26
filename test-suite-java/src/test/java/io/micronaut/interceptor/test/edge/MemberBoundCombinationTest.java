package io.micronaut.interceptor.test.edge;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberBoundCombinationTest {

    /**
     * An interceptor whose binding declares a different member value does not bind, and does not start binding
     * because an interceptor named directly happens to intercept the same method.
     */
    @Test
    void aBindingThatDiffersInAMemberDoesNotBindAlongsideANamedInterceptor() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Log.RECORDED.clear();

            assertEquals("Hello Denis", context.getBean(MemberBoundService.class).greet("Denis"));
            assertEquals(List.of("named first", "target got Denis"), Log.RECORDED);
        }
    }
}
