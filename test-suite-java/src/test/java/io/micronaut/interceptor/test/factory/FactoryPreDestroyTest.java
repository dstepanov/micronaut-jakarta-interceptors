package io.micronaut.interceptor.test.factory;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The destroy method a {@code @Factory} method names has to be invoked whether or not a binding on that method
 * makes the produced bean a proxy.
 */
class FactoryPreDestroyTest {

    @Test
    void theDestroyMethodOfAnInterceptedProducedBeanIsInvoked() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Disposable disposable = context.getBean(Disposable.class);
            Calls.clear();

            disposable.use();
            context.destroyBean(disposable);

            assertEquals(List.of("traced", "use", "close"), List.copyOf(Calls.RECORDED));
        }
    }
}
