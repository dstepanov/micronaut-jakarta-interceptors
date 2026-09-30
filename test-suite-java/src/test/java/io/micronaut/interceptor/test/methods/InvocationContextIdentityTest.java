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
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.runtime.InterceptorMethods;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 2.3: the same {@code InvocationContext} instance is passed to each interceptor method of one invocation. That holds
 * for the interceptor methods an interceptor class and its superclasses declare, which are invoked one after another,
 * and across the interceptor classes of a chain, whether the chain is one the runtime resolved or the methods of one
 * interceptor class are invoked directly.
 */
class InvocationContextIdentityTest {

    static final List<InvocationContext> SEEN = new ArrayList<>();

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
        SEEN.clear();
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface Identified {
    }

    public static class BaseIdentifyingInterceptor {
        @AroundInvoke
        public Object base(InvocationContext context) throws Exception {
            SEEN.add(context);
            return context.proceed();
        }
    }

    @Identified
    @Interceptor
    @Priority(100)
    public static class IdentifyingInterceptor extends BaseIdentifyingInterceptor {
        @AroundInvoke
        public Object own(InvocationContext context) throws Exception {
            SEEN.add(context);
            return context.proceed();
        }
    }

    @Identified
    @Interceptor
    @Priority(200)
    public static class LaterIdentifyingInterceptor {
        @AroundInvoke
        public Object later(InvocationContext context) throws Exception {
            SEEN.add(context);
            return context.proceed();
        }
    }

    @Singleton
    @Identified
    public static class IdentifiedService {
        public String work() {
            return "worked";
        }
    }

    @Test
    void theInterceptorMethodsOfAChainSeeTheSameContext() {
        assertEquals("worked", context.getBean(IdentifiedService.class).work());

        assertEquals(3, SEEN.size());
        assertSame(SEEN.get(0), SEEN.get(1));
        assertSame(SEEN.get(0), SEEN.get(2));
    }

    @Test
    void theInterceptorMethodsOfAClassInvokedDirectlySeeTheSameContext() throws Exception {
        InterceptorMethods methods = InterceptorMethods.of(
            context.getBeanDefinition(IdentifyingInterceptor.class), InterceptionKind.AROUND_INVOKE);

        assertEquals("target", methods.invoke(context.getBean(IdentifyingInterceptor.class), new Invocation()));

        assertEquals(2, SEEN.size());
        assertSame(SEEN.get(0), SEEN.get(1));
    }

    @Test
    void anInterceptorMethodInvokedDirectlyMayProceedAgain() throws Exception {
        InterceptorMethods methods = InterceptorMethods.of(
            context.getBeanDefinition(IdentifyingInterceptor.class), InterceptionKind.AROUND_INVOKE);
        Object interceptor = context.getBean(IdentifyingInterceptor.class);
        Invocation invocation = new Invocation();

        assertEquals("target", methods.invoke(interceptor, invocation));
        assertEquals("target", methods.invoke(interceptor, invocation));

        assertEquals(4, SEEN.size());
        assertEquals(2, invocation.proceeded);
    }

    /** An invocation that is not one of the runtime's own. */
    private static final class Invocation implements InvocationContext {
        private final Map<String, Object> data = new HashMap<>();
        private Object[] parameters = new Object[0];
        private int proceeded;

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
        public Object proceed() {
            proceeded++;
            return "target";
        }
    }
}
