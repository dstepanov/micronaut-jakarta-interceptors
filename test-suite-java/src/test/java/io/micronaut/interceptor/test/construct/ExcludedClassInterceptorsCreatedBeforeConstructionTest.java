package io.micronaut.interceptor.test.construct;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.Interceptors;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2.3 da) has injection completed on every interceptor class associated with the target class before
 * an {@code @AroundConstruct} method runs. The association is read from the constructor's interception, which is
 * not the same set: the constructor may exclude the class interceptors, and an interceptor with only an
 * {@code @AroundTimeout} method has no AROUND reference.
 */
class ExcludedClassInterceptorsCreatedBeforeConstructionTest {

    static final List<String> RECORDED = Collections.synchronizedList(new ArrayList<>());

    @Test
    void aClassInterceptorExcludedFromTheConstructorIsStillInjectedBeforeTheConstruction() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RECORDED.clear();
            assertEquals("done", context.getBean(ExcludingService.class).work());

            assertInjectedBeforeConstruction("invoke-only injected");
        }
    }

    @Test
    void aTimeoutOnlyClassInterceptorIsInjectedBeforeTheConstruction() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RECORDED.clear();
            context.getBean(TimedBuiltService.class);

            assertInjectedBeforeConstruction("timeout-only injected");
        }
    }

    private static void assertInjectedBeforeConstruction(String injected) {
        List<String> recorded = List.copyOf(RECORDED);
        int injectedAt = recorded.indexOf(injected);
        int constructingAt = recorded.indexOf("construct before proceed");
        assertTrue(constructingAt >= 0, "the construction was intercepted: " + recorded);
        assertTrue(injectedAt >= 0 && injectedAt < constructingAt,
            "[" + injected + "] before the around-construct method: " + recorded);
    }

    @Singleton
    public static class Collaborator {
    }

    /** Named by the class; takes no part in the construction. */
    public static class InvokeOnlyInterceptor {

        public InvokeOnlyInterceptor() {
            RECORDED.add("invoke-only created");
        }

        @Inject
        void inject(Collaborator collaborator) {
            RECORDED.add("invoke-only injected");
        }

        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    /** Named by the constructor alone. */
    public static class ConstructOnlyInterceptor {

        @AroundConstruct
        public void construct(InvocationContext context) throws Exception {
            RECORDED.add("construct before proceed");
            context.proceed();
        }
    }

    @Prototype
    @Interceptors(InvokeOnlyInterceptor.class)
    public static class ExcludingService {

        @ExcludeClassInterceptors
        @Interceptors(ConstructOnlyInterceptor.class)
        public ExcludingService() {
            RECORDED.add("target constructed");
        }

        public String work() {
            return "done";
        }
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TimedAndBuilt {
    }

    /** Bound to the class, declaring nothing but an around-timeout method. */
    @Interceptor
    @TimedAndBuilt
    @Priority(Interceptor.Priority.APPLICATION)
    public static class TimeoutOnlyInterceptor {

        public TimeoutOnlyInterceptor() {
            RECORDED.add("timeout-only created");
        }

        @Inject
        void inject(Collaborator collaborator) {
            RECORDED.add("timeout-only injected");
        }

        @AroundTimeout
        public Object timeout(InvocationContext context) throws Exception {
            return context.proceed();
        }
    }

    @Interceptor
    @TimedAndBuilt
    @Priority(Interceptor.Priority.APPLICATION + 1)
    public static class BuildInterceptor {

        @AroundConstruct
        public void construct(InvocationContext context) throws Exception {
            RECORDED.add("construct before proceed");
            context.proceed();
        }
    }

    @Prototype
    @TimedAndBuilt
    public static class TimedBuiltService {

        public TimedBuiltService() {
            RECORDED.add("target constructed");
        }

        @Scheduled(fixedDelay = "1h", initialDelay = "1h")
        public void onSchedule() {
        }
    }
}
