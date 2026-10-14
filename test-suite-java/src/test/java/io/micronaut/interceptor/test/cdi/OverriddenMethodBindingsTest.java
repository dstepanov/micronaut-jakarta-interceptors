package io.micronaut.interceptor.test.cdi;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.MicronautInvocationContext;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What a method declares is declared by that method: an override of it that does not declare it again does not have
 * it. That holds for the bindings of the overridden method where the overriding class is bound itself - the bindings
 * of the class are then the only ones of the override - and for the interceptor classes the overridden method names,
 * and for its exclusion of the interceptors of the class.
 */
class OverriddenMethodBindingsTest {

    /** Bound at the class, and overriding a method that declares a binding of its own. */
    @Singleton
    @Monitored
    public static class BoundOverridingService extends GuardedBase {
        @Override
        public void guarded() {
            Calls.RECORDED.add("overridden");
        }
    }

    public static class NamedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            Calls.RECORDED.add("named");
            return context.proceed();
        }
    }

    public static class NamingBase {
        @Interceptors(NamedInterceptor.class)
        public void named() {
            Calls.RECORDED.add("base");
        }

        @ExcludeClassInterceptors
        public void excluding() {
            Calls.RECORDED.add("base");
        }
    }

    @Singleton
    @Monitored
    public static class NamingOverridingService extends NamingBase {
        @Override
        public void named() {
            Calls.RECORDED.add("overridden");
        }
    }

    public static class ClassLevelInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            Calls.RECORDED.add("class level");
            return context.proceed();
        }
    }

    @Singleton
    @Interceptors(ClassLevelInterceptor.class)
    public static class ExclusionOverridingService extends NamingBase {
        @Override
        public void excluding() {
            Calls.RECORDED.add("overridden");
        }
    }

    @InterceptorBinding
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Remarked {
        @Nonbinding
        String value() default "";
    }

    @Remarked
    @Interceptor
    public static class RemarkReportingInterceptor {

        static final List<String> REMARKS = new ArrayList<>();

        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            for (AnnotationValue<?> value : ((MicronautInvocationContext) context).getInterceptorBindingValues()) {
                if (value.getAnnotationName().equals(Remarked.class.getName())) {
                    REMARKS.add(value.stringValue().orElse(""));
                }
            }
            return context.proceed();
        }
    }

    public interface RemarkedContract {
        @Remarked("of the overridden method")
        void remarked();
    }

    @Singleton
    @Remarked("of the class")
    public static class RemarkedOverridingService implements RemarkedContract {
        @Override
        @Monitored
        public void remarked() {
        }
    }

    @Singleton
    @Remarked("of the class")
    public static class RemarkedPlainOverridingService implements RemarkedContract {
        @Override
        public void remarked() {
        }
    }

    /**
     * The binding of an override bound at its class is reported as the class declared it, not as the overridden
     * method did, although the two are the same binding: the overridden method's declaration is not the override's,
     * whether the override declares a binding of its own or none.
     */
    @Test
    void theBindingOfAClassIsReportedAsTheClassDeclaredItRatherThanAsAnOverriddenMethodDid() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RemarkReportingInterceptor.REMARKS.clear();
            context.getBean(RemarkedOverridingService.class).remarked();
            context.getBean(RemarkedPlainOverridingService.class).remarked();

            assertEquals(List.of("of the class", "of the class"), List.copyOf(RemarkReportingInterceptor.REMARKS));
        }
    }

    @Test
    void theBindingOfAnOverriddenMethodDoesNotComeBackWhereTheClassIsBound() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(BoundOverridingService.class).guarded();

            assertEquals(List.of("monitored", "overridden"), List.copyOf(Calls.RECORDED));
        }
    }

    @Test
    void theBindingOfAnOverriddenMethodIsNotReportedWhereTheClassIsBound() {
        try (ApplicationContext context = ApplicationContext.run()) {
            context.getBean(BoundOverridingService.class).guarded();

            assertEquals(List.of("values [Monitored]", "instances [Monitored]", "secure false 0"),
                List.copyOf(MonitoringInterceptor.REPORTED));
        }
    }

    @Test
    void theInterceptorClassesOfAnOverriddenMethodAreNotInherited() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(NamingOverridingService.class).named();

            assertEquals(List.of("monitored", "overridden"), List.copyOf(Calls.RECORDED));
        }
    }

    @Test
    void theExclusionOfAnOverriddenMethodIsNotInherited() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Calls.clear();
            context.getBean(ExclusionOverridingService.class).excluding();

            assertEquals(List.of("class level", "overridden"), List.copyOf(Calls.RECORDED));
        }
    }
}
