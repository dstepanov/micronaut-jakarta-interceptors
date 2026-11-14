package io.micronaut.interceptor.test.advised;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.AdvisedBeanType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvisedInterceptorClassTest {

    /**
     * Section 5.2: an interceptor class both named with {@code @Interceptors} and bound by a binding of the same
     * element is invoked once. That holds for an interceptor class Micronaut proxies as well, whose bean type is the
     * subclass Micronaut generates rather than the class the element names.
     */
    @Test
    void anAdvisedInterceptorClassNamedAndBoundIsInvokedOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            AdvisedCountingInterceptor.RECORDED.clear();
            assertTrue(context.getBeanDefinition(AdvisedCountingInterceptor.class) instanceof AdvisedBeanType<?>);

            NamedAndBoundService service = context.getBean(NamedAndBoundService.class);
            assertEquals("done", service.work());
            assertEquals("bound", service.bound());

            assertEquals(List.of("counted work", "counted bound"), AdvisedCountingInterceptor.RECORDED);
        }
    }
}
