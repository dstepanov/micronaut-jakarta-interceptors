package io.micronaut.interceptor.test.scope;

import io.micronaut.aop.Around;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.context.scope.Refreshable;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An interceptor class Micronaut resolves through a proxy - of a scope that resolves through one, or declared
 * {@code @Around(proxyTarget = true)} - is one interceptor class, whose interceptor methods are those its own
 * definition describes, not the methods the proxy exposes. The proxy intercepts nothing of the interceptor class:
 * the bindings of an interceptor class are the ones it binds to others.
 */
class ScopedInterceptorClassTest {

    static final List<String> CALLS = new ArrayList<>();

    @Test
    void aProxiedInterceptorClassIsInvokedOnce() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals("scoped worked", context.getBean(ScopedService.class).work());
            assertEquals("utility", context.getBean(ScopedInterceptor.class).utility());
        }
    }

    @Test
    void anInterceptorClassThatIsProxiedIsNotInterceptedByItself() {
        try (ApplicationContext context = ApplicationContext.run()) {
            CALLS.clear();
            assertEquals("utility", context.getBean(TargetProxiedInterceptor.class).utility());
            assertEquals("utility", context.getBean(RefreshableInterceptor.class).utility());
            assertEquals(List.of(), CALLS);

            assertEquals("worked", context.getBean(ProxiedService.class).work());
            assertEquals(List.of("refreshable work", "target proxied work"), CALLS);
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface ScopedGuarded {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface ProxiedGuarded {
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

    @Interceptor
    @Around(proxyTarget = true)
    @ProxiedGuarded
    public static class TargetProxiedInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            CALLS.add("target proxied " + context.getMethod().getName());
            return context.proceed();
        }

        public String utility() {
            return "utility";
        }
    }

    @Interceptor
    @Refreshable
    @ProxiedGuarded
    public static class RefreshableInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext context) throws Exception {
            CALLS.add("refreshable " + context.getMethod().getName());
            return context.proceed();
        }

        public String utility() {
            return "utility";
        }
    }

    @Singleton
    @ProxiedGuarded
    public static class ProxiedService {
        public String work() {
            return "worked";
        }
    }
}
