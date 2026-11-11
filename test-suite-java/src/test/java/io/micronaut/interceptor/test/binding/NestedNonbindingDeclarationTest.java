package io.micronaut.interceptor.test.binding;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.MicronautInvocationContext;
import jakarta.enterprise.util.Nonbinding;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A method redeclaring the binding of its class, differing from it only in a member that does not bind of an
 * annotation a member of the binding holds, is bound alike but reports its own declaration, both as values and as
 * annotations.
 */
class NestedNonbindingDeclarationTest {

    @Test
    void theMethodReportsItsOwnDeclarationOfANestedMemberThatDoesNotBind() {
        try (ApplicationContext context = ApplicationContext.run()) {
            DetailInterceptor.RECORDED.clear();
            assertEquals("redeclared", context.getBean(DetailedService.class).redeclared());
            assertEquals("inherited", context.getBean(DetailedService.class).inherited());

            assertEquals(List.of(
                    "redeclared values method", "redeclared annotations method",
                    "inherited values class", "inherited annotations class"),
                DetailInterceptor.RECORDED);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    public @interface Detail {
        String value();

        @Nonbinding
        String note() default "";
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Detailed {
        Detail value();
    }

    @Interceptor
    @Detailed(@Detail("x"))
    public static class DetailInterceptor {

        static final List<String> RECORDED = new CopyOnWriteArrayList<>();

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            String method = context.getMethod().getName();
            for (AnnotationValue<?> binding : ((MicronautInvocationContext) context).getInterceptorBindingValues()) {
                if (binding.getAnnotationName().equals(Detailed.class.getName())) {
                    RECORDED.add(method + " values " + binding.getAnnotation("value")
                        .flatMap(detail -> detail.stringValue("note")).orElse("(none)"));
                }
            }
            Detailed detailed = context.getInterceptorBinding(Detailed.class);
            RECORDED.add(method + " annotations " + (detailed == null ? "(none)" : detailed.value().note()));
            return context.proceed();
        }
    }

    @Singleton
    @Detailed(@Detail(value = "x", note = "class"))
    public static class DetailedService {

        @Detailed(@Detail(value = "x", note = "method"))
        public String redeclared() {
            return "redeclared";
        }

        public String inherited() {
            return "inherited";
        }
    }
}
