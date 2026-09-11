package io.micronaut.interceptor.test.factory;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.inject.BeanDefinition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * An interceptor class declaring {@code @Interceptor} may also be produced by a factory, and the factory then takes
 * the place of the definition the module declares for the class rather than the two being ambiguous.
 */
class InterceptorClassFactoryTest {

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
    void theFactoryProducesTheInterceptorClassBean() {
        assertEquals("factory", context.getBean(AuditingInterceptor.class).source());
    }

    @Test
    void theInstanceTheFactoryProducedIntercepts() {
        assertEquals("work", context.getBean(AuditedService.class).work());

        assertEquals(List.of("audited by factory"), Calls.RECORDED);
    }

    /**
     * A class the application declares a bean of its own is left as it declared it: the module makes it neither a
     * secondary definition nor a prototype.
     */
    @Test
    void anInterceptorClassDeclaredABeanKeepsWhatItDeclared() {
        BeanDefinition<SingletonInterceptor> definition = context.getBeanDefinition(SingletonInterceptor.class);

        assertFalse(definition.hasStereotype(Secondary.class));
        assertSame(context.getBean(SingletonInterceptor.class), context.getBean(SingletonInterceptor.class));
    }
}
