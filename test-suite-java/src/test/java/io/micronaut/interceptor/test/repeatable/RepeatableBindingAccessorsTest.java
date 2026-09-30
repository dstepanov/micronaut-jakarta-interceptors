package io.micronaut.interceptor.test.repeatable;

import io.micronaut.context.ApplicationContext;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * a repeatable binding the method declares replaces every occurrence of that type its class declares,
 * which is what the chain is selected by. The binding accessors of the invocation context have to report the same
 * effective bindings, not the union of the class and the method.
 */
class RepeatableBindingAccessorsTest {

    static final List<String> SEEN = Collections.synchronizedList(new ArrayList<>());

    @Test
    void theTypedAccessorReportsOnlyTheBindingsOfTheMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            SEEN.clear();
            assertEquals("done", context.getBean(TaggedService.class).work());

            assertEquals(List.of("typed [method]", "untyped [method]"), SEEN);
        }
    }

    @InterceptorBinding
    @Repeatable(VerifyTags.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface VerifyTag {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface VerifyTags {
        VerifyTag[] value();
    }

    @Interceptor
    @VerifyTag("method")
    public static class MethodTagInterceptor {

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            SEEN.add("typed " + values(context.getInterceptorBindings(VerifyTag.class)));
            SEEN.add("untyped " + values(context.getInterceptorBindings()));
            return context.proceed();
        }

        private static Set<String> values(Set<? extends Annotation> bindings) {
            Set<String> values = new TreeSet<>();
            for (Annotation binding : bindings) {
                if (binding instanceof VerifyTag tag) {
                    values.add(tag.value());
                }
            }
            return values;
        }
    }

    @Interceptor
    @VerifyTag("class")
    public static class ClassTagInterceptor {

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            SEEN.add("class interceptor");
            return context.proceed();
        }
    }

    @Singleton
    @VerifyTag("class")
    public static class TaggedService {

        @VerifyTag("method")
        public String work() {
            return "done";
        }
    }
}
