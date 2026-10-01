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
package io.micronaut.interceptor.test.methods;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.Order;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.spi.InterceptorMethods;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interceptor methods of an interceptor class, read and invoked without a chain being resolved: the way a module
 * that hands an interceptor out to be invoked directly reads one, which has to be the way the runtime itself does.
 */
class InterceptorMethodsTest {

    static final List<String> CALLS = new ArrayList<>();

    private static ApplicationContext context;

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run();
    }

    @AfterAll
    static void stopContext() {
        context.close();
    }

    @BeforeEach
    void reset() {
        CALLS.clear();
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Stacked {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Refusing {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Clocked {
    }

    public static class BaseStackedInterceptor {
        @AroundInvoke
        private Object around(InvocationContext context) throws Exception {
            CALLS.add("base around");
            return context.proceed();
        }
    }

    @Stacked
    @Interceptor
    @Priority(700)
    public static class StackedInterceptor extends BaseStackedInterceptor {
        @AroundInvoke
        private Object around(InvocationContext context) throws Exception {
            CALLS.add("own around");
            Object[] parameters = context.getParameters();
            context.setParameters(new Object[] {parameters[0] + " seen"});
            return "[" + context.proceed() + "]";
        }
    }

    @Refusing
    @Interceptor
    @Order(30)
    public static class RefusingInterceptor {
        static Exception failure = new IOException("unset");

        @AroundInvoke
        private Object refuse(InvocationContext context) throws Exception {
            throw failure;
        }
    }

    @Clocked
    @Interceptor
    public static class ClockedInterceptor {
        @AroundInvoke
        Object business(InvocationContext context) throws Exception {
            CALLS.add("business");
            return context.proceed();
        }

        @AroundTimeout
        Object timeout(InvocationContext context) throws Exception {
            CALLS.add("timeout");
            return context.proceed();
        }
    }

    @Test
    void privateMethodsOfOneNameInAClassAndItsSuperclassAreBothInvokedSuperclassFirst() throws Exception {
        InterceptorMethods methods = methodsOf(StackedInterceptor.class, InterceptionKind.AROUND_INVOKE);
        Invocation invocation = new Invocation("given");

        Object result = methods.invoke(context.getBean(StackedInterceptor.class), invocation);

        assertEquals(List.of("base around", "own around", "proceeded"), CALLS);
        assertEquals("[target]", result);
        assertArrayEquals(new Object[] {"given seen"}, invocation.getParameters(),
            "what an interceptor method sets reaches the context of the invocation");
    }

    @Test
    void whatAPrivateInterceptorMethodThrowsArrivesAsItWasThrown() {
        InterceptorMethods methods = methodsOf(RefusingInterceptor.class, InterceptionKind.AROUND_INVOKE);
        RefusingInterceptor interceptor = context.getBean(RefusingInterceptor.class);

        IOException checked = new IOException("refused");
        RefusingInterceptor.failure = checked;
        assertSame(checked, assertThrows(IOException.class, () -> methods.invoke(interceptor, new Invocation("x"))));

        IllegalStateException unchecked = new IllegalStateException("refused");
        RefusingInterceptor.failure = unchecked;
        assertSame(unchecked,
            assertThrows(IllegalStateException.class, () -> methods.invoke(interceptor, new Invocation("x"))));
        assertEquals(List.of(), CALLS);
    }

    @Test
    void whatTheInvocationThrowsTravelsThroughTheMethodsAsItWasThrown() {
        InterceptorMethods methods = methodsOf(StackedInterceptor.class, InterceptionKind.AROUND_INVOKE);
        IOException thrown = new IOException("from the target");
        Invocation invocation = new Invocation("given");
        invocation.failure = thrown;

        assertSame(thrown, assertThrows(IOException.class,
            () -> methods.invoke(context.getBean(StackedInterceptor.class), invocation)));
    }

    @Test
    void aClassWithoutAnAroundTimeoutMethodDeclaresNoMethodsOfThatKind() {
        // that a chain falls back on its @AroundInvoke methods for a timeout is the chain's doing
        assertTrue(methodsOf(StackedInterceptor.class, InterceptionKind.AROUND_TIMEOUT).isEmpty());
        assertFalse(methodsOf(StackedInterceptor.class, InterceptionKind.AROUND_INVOKE).isEmpty());
    }

    @Test
    void theAroundTimeoutMethodsOfAClassThatDeclaresOneAreTheOnesOfThatKind() throws Exception {
        InterceptorMethods methods = methodsOf(ClockedInterceptor.class, InterceptionKind.AROUND_TIMEOUT);

        methods.invoke(context.getBean(ClockedInterceptor.class), new Invocation("given"));
        assertEquals(List.of("timeout", "proceeded"), CALLS);
    }

    @Test
    void aKindTheClassDeclaresNoMethodOfHasNoMethodsAndProceeds() throws Exception {
        InterceptorMethods methods = methodsOf(StackedInterceptor.class, InterceptionKind.POST_CONSTRUCT);

        assertTrue(methods.isEmpty());
        assertEquals("target", methods.invoke(context.getBean(StackedInterceptor.class), new Invocation("given")));
        assertEquals(List.of("proceeded"), CALLS);
    }

    @Test
    void thePriorityIsThatOfTheSpecificationThenTheOrderOfMicronautThenTheDefault() {
        assertEquals(700, InterceptorMethods.priorityOf(context.getBeanDefinition(StackedInterceptor.class)));
        assertEquals(30, InterceptorMethods.priorityOf(context.getBeanDefinition(RefusingInterceptor.class)));
        assertEquals(Interceptor.Priority.APPLICATION,
            InterceptorMethods.priorityOf(context.getBeanDefinition(ClockedInterceptor.class)));
    }

    private static InterceptorMethods methodsOf(Class<?> interceptorClass, InterceptionKind kind) {
        return InterceptorMethods.of(context.getBeanDefinition(interceptorClass), kind);
    }

    /** An invocation that is not one of the runtime's own. */
    private static final class Invocation implements InvocationContext {
        private final Map<String, Object> data = new HashMap<>();
        private Object[] parameters;
        private Exception failure;

        Invocation(Object parameter) {
            this.parameters = new Object[] {parameter};
        }

        @Override
        public Object getTarget() {
            return this;
        }

        @Override
        public Object getTimer() {
            return null;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public Constructor<?> getConstructor() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return parameters;
        }

        @Override
        public void setParameters(Object[] params) {
            this.parameters = params;
        }

        @Override
        public Map<String, Object> getContextData() {
            return data;
        }

        @Override
        public Set<Annotation> getInterceptorBindings() {
            return Set.of();
        }

        @Override
        public Object proceed() throws Exception {
            if (failure != null) {
                throw failure;
            }
            CALLS.add("proceeded");
            return "target";
        }
    }
}
