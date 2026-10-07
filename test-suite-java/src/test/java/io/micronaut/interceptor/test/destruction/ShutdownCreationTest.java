package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import jakarta.annotation.PreDestroy;
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
 * An object first created while the context shuts down - by the destruction callback of another bean - is
 * intercepted as any other: its interceptor instances are created with it, interpose on its methods and on its
 * destruction, and are destroyed after it.
 */
class ShutdownCreationTest {

    static final List<String> RECORDED = Collections.synchronizedList(new ArrayList<>());

    @Test
    void interceptsAnObjectFirstCreatedDuringShutdown() {
        RECORDED.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(ShutdownCaller.class);
        }
        assertEquals(List.of(
            "caller destroyed",
            "late interceptor created",
            "around lateWork",
            "late work done",
            "intercepted late destruction",
            "late target destroyed",
            "late interceptor destroyed"), RECORDED);
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Late {
    }

    @Interceptor
    @Late
    public static class LateInterceptor {

        public LateInterceptor() {
            RECORDED.add("late interceptor created");
        }

        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            RECORDED.add("around " + context.getMethod().getName());
            return context.proceed();
        }

        @PreDestroy
        public void interceptDestruction(InvocationContext context) throws Exception {
            RECORDED.add("intercepted late destruction");
            context.proceed();
        }

        @PreDestroy
        void close() {
            RECORDED.add("late interceptor destroyed");
        }
    }

    @Singleton
    @Late
    public static class LateTarget {

        public void lateWork() {
            RECORDED.add("late work done");
        }

        @PreDestroy
        void close() {
            RECORDED.add("late target destroyed");
        }
    }

    @Singleton
    public static class ShutdownCaller {

        private final BeanContext context;

        public ShutdownCaller(BeanContext context) {
            this.context = context;
        }

        @PreDestroy
        void close() {
            RECORDED.add("caller destroyed");
            context.getBean(LateTarget.class).lateWork();
        }
    }
}
