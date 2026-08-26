package io.micronaut.interceptor.test.defaults;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a bean of an interceptor class does, and does not, take part in.
 */
class CustomInterceptorBeanTest {

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
        Log.RECORDED.clear();
    }

    /**
     * An interceptor that declares no binding intercepts nothing. It does not become a default interceptor: the
     * specification has default interceptors declared in a deployment descriptor, and an interceptor without a
     * binding is left inert rather than applied everywhere.
     */
    @Test
    void anInterceptorWithoutABindingInterceptsNothing() {
        assertEquals("marked", context.getBean(MarkedBean.class).work());

        assertEquals(List.of(), Log.RECORDED);
    }

    /**
     * A bean that declares no interception of its own is never intercepted, whatever interceptors the context
     * holds.
     */
    @Test
    void aBeanThatDeclaresNoInterceptionIsNeverIntercepted() {
        assertEquals("plain", context.getBean(PlainBean.class).work());

        assertEquals(List.of(), Log.RECORDED);
    }

    /**
     * An interceptor class may be produced by a factory, so that the application configures the instance itself.
     * The instance the factory produced is the one that intercepts, and its interceptor methods are still invoked
     * through the executable methods generated for its class.
     */
    @Test
    void interceptsWithTheInstanceAFactoryProduced() {
        assertEquals("factoryUsing", context.getBean(FactoryUsingBean.class).work());

        assertEquals(List.of("factory made interceptor configured at runtime"), Log.RECORDED);
    }
}
