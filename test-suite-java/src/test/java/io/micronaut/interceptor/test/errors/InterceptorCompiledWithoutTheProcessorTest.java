package io.micronaut.interceptor.test.errors;

import io.micronaut.context.ApplicationContext;
import io.micronaut.interceptor.test.external.UnprocessedBeanInterceptor;
import io.micronaut.interceptor.test.external.UnprocessedPlainInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interceptor class named by {@code @Interceptors} that was compiled without the Jakarta Interceptors processor
 * cannot be invoked: nothing recorded its interceptor methods. The chain it is named in fails to resolve, naming the
 * class and what it has to be compiled with, rather than resolving without it and leaving the element silently
 * unintercepted.
 */
class InterceptorCompiledWithoutTheProcessorTest {

    @Test
    void anInterceptorClassWithADefinitionThatDoesNotDescribeItIsReported() {
        assertReported(NamesUnprocessedBeanInterceptor.class, UnprocessedBeanInterceptor.class);
    }

    @Test
    void anInterceptorClassWithNoDefinitionIsReported() {
        assertReported(NamesUnprocessedPlainInterceptor.class, UnprocessedPlainInterceptor.class);
    }

    private static void assertReported(Class<?> intercepted, Class<?> interceptorClass) {
        try (ApplicationContext context = ApplicationContext.run()) {
            // the chains of a bean are resolved as the bean is created
            RuntimeException failure = assertThrows(RuntimeException.class, () -> context.getBean(intercepted));

            String messages = messagesOf(failure);
            assertTrue(messages.contains(interceptorClass.getName()), messages);
            assertTrue(messages.contains("micronaut-jakarta-interceptors-processor"), messages);
        }
    }

    private static String messagesOf(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = failure; current != null; current = current.getCause()) {
            messages.append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }
}
