/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.interceptor.test.ordering;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.spi.BoundInterceptorEnablement;
import io.micronaut.interceptor.spi.InterceptorBindings;
import io.micronaut.interceptor.spi.InterceptorMethods;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Another module asks {@link InterceptorBindings#boundInterceptors} for the interceptor classes bound to a set of
 * bindings, and is answered with the selection the chains make: the enabled interceptor classes every binding of
 * which the point declares, in the order a chain invokes them in.
 */
class BoundInterceptorsTest {

    static final String DISABLING = "test.bound-interceptors.disabling";

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Picked {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Paired {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Unpicked {
    }

    @Picked
    @Interceptor
    public static class AlphaPickedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "alpha " + context.proceed();
        }
    }

    @Picked
    @Interceptor
    @Priority(Interceptor.Priority.APPLICATION - 10)
    public static class EarlyPickedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "early " + context.proceed();
        }
    }

    @Picked
    @Interceptor
    public static class ZuluPickedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "zulu " + context.proceed();
        }
    }

    @Picked
    @Paired
    @Interceptor
    @Priority(Interceptor.Priority.APPLICATION + 10)
    public static class PairedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "paired " + context.proceed();
        }
    }

    @Unpicked
    @Interceptor
    public static class UnpickedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            return "unpicked " + context.proceed();
        }
    }

    @Singleton
    @Picked
    public static class PickedService {
        public String work() {
            return "worked";
        }
    }

    @Singleton
    @Picked
    @Paired
    public static class PairedService {
        public String work() {
            return "worked";
        }
    }

    /** Leaves out the interceptor class named after zulu, and only where the test asks for it. */
    @Singleton
    @Requires(property = DISABLING, value = "true")
    public static class DisablingEnablement implements BoundInterceptorEnablement {
        @Override
        public boolean isEnabled(BeanDefinition<?> interceptor) {
            return interceptor.getBeanType() != ZuluPickedInterceptor.class;
        }
    }

    @SafeVarargs
    private static InterceptorBindings point(Class<? extends Annotation>... bindings) {
        List<AnnotationValue<?>> values = new ArrayList<>(bindings.length);
        for (Class<? extends Annotation> binding : bindings) {
            values.add(AnnotationValue.builder(binding).build());
        }
        return InterceptorBindings.ofValues(values);
    }

    private static List<Class<?>> classes(List<BeanDefinition<?>> definitions) {
        List<Class<?>> classes = new ArrayList<>(definitions.size());
        for (BeanDefinition<?> definition : definitions) {
            classes.add(definition.getBeanType());
        }
        return classes;
    }

    @Test
    void theInterceptorsEveryBindingOfWhichThePointDeclaresInTheOrderOfTheChain() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertEquals(
                List.of(EarlyPickedInterceptor.class, AlphaPickedInterceptor.class, ZuluPickedInterceptor.class),
                classes(InterceptorBindings.boundInterceptors(context, point(Picked.class))));
            assertEquals("early alpha zulu worked", context.getBean(PickedService.class).work());

            assertEquals(
                List.of(EarlyPickedInterceptor.class, AlphaPickedInterceptor.class, ZuluPickedInterceptor.class,
                    PairedInterceptor.class),
                classes(InterceptorBindings.boundInterceptors(context, point(Paired.class, Picked.class))));
            assertEquals("early alpha zulu paired worked", context.getBean(PairedService.class).work());

            assertEquals(List.of(UnpickedInterceptor.class),
                classes(InterceptorBindings.boundInterceptors(context, point(Unpicked.class))));
            assertEquals(List.of(), InterceptorBindings.boundInterceptors(context, point()));
        }
    }

    @Test
    void anInterceptorAnEnablementDoesNotEnableIsLeftOut() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(DISABLING, "true"))) {
            assertEquals(List.of(EarlyPickedInterceptor.class, AlphaPickedInterceptor.class),
                classes(InterceptorBindings.boundInterceptors(context, point(Picked.class))));
            assertEquals("early alpha worked", context.getBean(PickedService.class).work());
        }
    }

    @Test
    void eachDefinitionDescribesItsInterceptorClass() {
        try (ApplicationContext context = ApplicationContext.run()) {
            List<BeanDefinition<?>> bound = InterceptorBindings.boundInterceptors(context, point(Picked.class, Paired.class));

            BeanDefinition<?> paired = bound.get(bound.size() - 1);
            assertEquals(PairedInterceptor.class, paired.getBeanType());
            assertEquals(point(Paired.class, Picked.class), InterceptorBindings.of(paired));
            assertFalse(InterceptorMethods.of(paired, InterceptionKind.AROUND_INVOKE).isEmpty());
        }
    }
}
