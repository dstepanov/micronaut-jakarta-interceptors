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
package io.micronaut.interceptor.test.index;

import io.micronaut.aop.Adapter;
import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.interceptor.annotation.JakartaInterceptorIndex;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prototype C: the interceptor classes of an application are found through the bean index of the context rather
 * than by reading every bean definition there is.
 *
 * <p>The processor declares {@code @Indexed(JakartaInterceptorIndex.class)} on every interceptor class, and the
 * runtime asks the context for the definitions of that type. Nothing else about the module changes: the same
 * aggregate advice resolves the same chains, in the same order, with the same instances.</p>
 */
final class InterceptorIndexTest {

    @Test
    void theIndexHoldsEveryInterceptorClassAndNothingElse() {
        try (ApplicationContext context = ApplicationContext.run()) {
            Collection<BeanDefinition<JakartaInterceptorIndex>> indexed =
                context.getBeanDefinitions(JakartaInterceptorIndex.class);

            // the definition of the index is the one of the adapted method, and it describes the interceptor
            // class that declared it
            List<String> adapted = indexed.stream()
                .map(d -> d.classValue(Adapter.class, Adapter.InternalAttributes.ADAPTED_BEAN)
                    .orElseThrow().getName())
                .toList();
            assertTrue(adapted.contains(IndexedFirst.class.getName()));
            assertTrue(adapted.contains(IndexedSecond.class.getName()));

            // and it carries the metadata of that class, which is what the chains are resolved by
            BeanDefinition<?> first = indexed.stream()
                .filter(d -> d.classValue(Adapter.class, Adapter.InternalAttributes.ADAPTED_BEAN)
                    .filter(IndexedFirst.class::equals).isPresent())
                .findFirst()
                .orElseThrow();
            assertTrue(first.hasAnnotation("jakarta.interceptor.Interceptor"));
            assertTrue(first.hasAnnotation(Indexable.class));
            assertEquals(10, first.intValue(Priority.class).orElseThrow());
        }
    }

    @Test
    void theIndexedInterceptorsStillInterceptInPriorityOrder() {
        Recorder.values.clear();
        try (ApplicationContext context = ApplicationContext.run()) {
            IndexedTarget target = context.getBean(IndexedTarget.class);
            assertEquals("done", target.work());
            assertEquals(List.of("first", "second", "target"), Recorder.values);
        }
    }
}

@Documented
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Indexable {
}

@Interceptor
@Indexable
@Priority(10)
class IndexedFirst {

    @AroundInvoke
    Object around(InvocationContext context) throws Exception {
        Recorder.values.add("first");
        return context.proceed();
    }
}

@Interceptor
@Indexable
@Priority(20)
class IndexedSecond {

    @AroundInvoke
    Object around(InvocationContext context) throws Exception {
        Recorder.values.add("second");
        return context.proceed();
    }
}

@Singleton
@Indexable
class IndexedTarget {

    String work() {
        Recorder.values.add("target");
        return "done";
    }
}

final class Recorder {

    static final List<String> values = new java.util.ArrayList<>();

    private Recorder() {
    }
}
