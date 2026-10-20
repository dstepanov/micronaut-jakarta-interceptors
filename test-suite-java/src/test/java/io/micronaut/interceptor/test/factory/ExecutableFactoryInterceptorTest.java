package io.micronaut.interceptor.test.factory;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An executable factory method adds a proxy definition of the interceptor class it produces. The proxy is not another
 * interceptor class, and the methods it exposes are not the ones the processor recorded on the class.
 */
class ExecutableFactoryInterceptorTest {

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @Test
    void aPublicInterceptorMethodIsInvokedOnce() {
        assertEquals("public worked", context.getBean(PublicService.class).work());
    }

    @Test
    void aPrivateInterceptorMethodIsFound() {
        assertEquals("private worked", context.getBean(PrivateService.class).work());
    }

    /**
     * An interceptor class is not intercepted, by its own bindings or any other, so the factory produces it as it
     * is rather than a proxy of it.
     */
    @Test
    void theProducedInterceptorIsNotIntercepted() {
        PublicInterceptor publicInterceptor = context.getBean(PublicInterceptor.class);
        PrivateInterceptor privateInterceptor = context.getBean(PrivateInterceptor.class);

        assertEquals("utility", publicInterceptor.utility());
        assertEquals("utility", privateInterceptor.utility());
        assertEquals(PublicInterceptor.class, publicInterceptor.getClass());
        assertEquals(PrivateInterceptor.class, privateInterceptor.getClass());
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface PublicGuarded {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface PrivateGuarded {
    }

    @Interceptor
    @PublicGuarded
    public static class PublicInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            return "public " + context.proceed();
        }

        public String utility() {
            return "utility";
        }
    }

    @Interceptor
    @PrivateGuarded
    public static class PrivateInterceptor {
        @AroundInvoke
        private Object intercept(InvocationContext context) throws Exception {
            return "private " + context.proceed();
        }

        public String utility() {
            return "utility";
        }
    }

    @Factory
    public static class InterceptorFactory {
        @Singleton
        @Executable
        public PublicInterceptor publicInterceptor() {
            return new PublicInterceptor();
        }

        @Singleton
        @Executable
        public PrivateInterceptor privateInterceptor() {
            return new PrivateInterceptor();
        }
    }

    @Singleton
    @PublicGuarded
    public static class PublicService {
        public String work() {
            return "worked";
        }
    }

    @Singleton
    @PrivateGuarded
    public static class PrivateService {
        public String work() {
            return "worked";
        }
    }
}
