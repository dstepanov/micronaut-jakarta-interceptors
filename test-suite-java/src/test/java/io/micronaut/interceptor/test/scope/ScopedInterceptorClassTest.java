package io.micronaut.interceptor.test.scope;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.context.scope.ThreadLocal;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An interceptor class of a scope Micronaut resolves through a proxy is one interceptor class, whose interceptor
 * methods are those its own definition describes, not the methods the proxy exposes.
 */
class ScopedInterceptorClassTest {

    @Test
    void aProxiedInterceptorClassIsInvokedOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("scoped worked", context.getBean(ScopedService.class).work());
            assertEquals("utility", context.getBean(ScopedInterceptor.class).utility());
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface ScopedGuarded {
    }

    @Interceptor
    @ThreadLocal
    @ScopedGuarded
    public static class ScopedInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            return "scoped " + context.proceed();
        }

        public String utility() {
            return "utility";
        }
    }

    @Singleton
    @ScopedGuarded
    public static class ScopedService {
        public String work() {
            return "worked";
        }
    }
}
