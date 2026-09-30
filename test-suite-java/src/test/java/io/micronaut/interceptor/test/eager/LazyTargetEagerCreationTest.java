package io.micronaut.interceptor.test.eager;

import io.micronaut.aop.Around;
import io.micronaut.context.ApplicationContext;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 2.3 creates every interceptor instance of an object when the object is created. The eager creation
 * the module does is covered for a proxy with an eager target (EagerCreationTest.methodOnlyProxyTarget); the same
 * shape with a lazily resolved target, whose methods are bound to separate interceptors, is the case here.
 */
class LazyTargetEagerCreationTest {

    static final List<String> CREATED = Collections.synchronizedList(new ArrayList<>());

    @Test
    void theEagerTargetCreatesBothInterceptorsWithTheTarget() {
        assertBothCreatedOnTheFirstCall(EagerTarget.class, bean -> ((EagerTarget) bean).first());
    }

    @Test
    void theLazyTargetCreatesBothInterceptorsWithTheTarget() {
        assertBothCreatedOnTheFirstCall(LazyTarget.class, bean -> ((LazyTarget) bean).first());
    }

    private static void assertBothCreatedOnTheFirstCall(Class<?> type, java.util.function.Function<Object, String> call) {
        CREATED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            Object bean = context.getBean(type);
            assertEquals("first", call.apply(bean));

            assertEquals(List.of("first", "second"), CREATED.stream().sorted().toList(),
                type.getSimpleName() + ": the interceptor of the method not called is created with the target");
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface First {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Second {
    }

    @Interceptor
    @First
    public static class FirstInterceptor {

        public FirstInterceptor() {
            CREATED.add("first");
        }

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Interceptor
    @Second
    public static class SecondInterceptor {

        public SecondInterceptor() {
            CREATED.add("second");
        }

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Singleton
    @Around(proxyTarget = true)
    public static class EagerTarget {

        @First
        public String first() {
            return "first";
        }

        @Second
        public String second() {
            return "second";
        }
    }

    @Singleton
    @Around(proxyTarget = true, lazy = true)
    public static class LazyTarget {

        @First
        public String first() {
            return "first";
        }

        @Second
        public String second() {
            return "second";
        }
    }
}
