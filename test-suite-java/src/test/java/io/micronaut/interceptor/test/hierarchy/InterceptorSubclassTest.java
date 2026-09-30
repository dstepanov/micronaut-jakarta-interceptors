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
package io.micronaut.interceptor.test.hierarchy;

import io.micronaut.context.ApplicationContext;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An interceptor class and a subclass of it that is an interceptor class too are two interceptor classes: each is
 * bound by the bindings it declares, and each intercepts with an instance of itself. The subclass is a bean of the
 * type of its superclass as well, and must not be taken for it - nor its instance, which overrides the interceptor
 * method.
 */
class InterceptorSubclassTest {

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
    @interface ParentGuard {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({TYPE, METHOD})
    @interface ChildGuard {
    }

    // named so that the subclass comes first wherever the two are ordered by name
    @Interceptor
    @ParentGuard
    @Priority(100)
    public static class ZuluParentInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("parent");
            return context.proceed();
        }
    }

    @Interceptor
    @ChildGuard
    @Priority(200)
    public static class AlphaChildInterceptor extends ZuluParentInterceptor {
        @Override
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("child");
            return context.proceed();
        }
    }

    @Singleton
    @ParentGuard
    public static class ParentGuardedService {
        public void action() {
        }
    }

    @Singleton
    @ChildGuard
    public static class ChildGuardedService {
        public void action() {
        }
    }

    @Singleton
    @Interceptors(ZuluParentInterceptor.class)
    public static class ParentNamingService {
        public void action() {
        }
    }

    @Test
    void theBindingOfTheSuperclassBindsTheSuperclass() {
        context.getBean(ParentGuardedService.class).action();

        assertEquals(List.of("parent"), CALLS);
    }

    @Test
    void theBindingOfTheSubclassBindsTheSubclass() {
        context.getBean(ChildGuardedService.class).action();

        assertEquals(List.of("child"), CALLS);
    }

    @Test
    void theSuperclassNamedByAnElementInterceptsAsItself() {
        context.getBean(ParentNamingService.class).action();

        assertEquals(List.of("parent"), CALLS);
    }
}
