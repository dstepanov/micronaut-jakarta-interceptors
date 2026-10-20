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
package io.micronaut.interceptor.test.factory;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A factory member whose type is an interface or an abstract class produces a bean of that type, which is bound by
 * the bindings the type declares as a bean of a class is by the bindings of the class. The produced object is of
 * another class, which declares nothing, and the interceptor of the binding runs once around each call.
 */
class ProducedAbstractTypeTest {

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
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Abstracted {
    }

    @Interceptor
    @Abstracted
    public static class AbstractedInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("abstracted " + context.getMethod().getName());
            return context.proceed();
        }
    }

    @Abstracted
    public interface BoundInterface {
        String work();
    }

    @Abstracted
    public abstract static class BoundAbstractClass {
        public abstract String work();
    }

    @Factory
    public static class AbstractTypeFactory {
        @Singleton
        BoundInterface boundInterface() {
            return () -> "work";
        }

        @Singleton
        BoundAbstractClass boundAbstractClass() {
            return new BoundAbstractClass() {
                @Override
                public String work() {
                    return "work";
                }
            };
        }
    }

    @Test
    void aProducedInterfaceIsBoundByItsBindings() {
        assertEquals("work", context.getBean(BoundInterface.class).work());

        assertEquals(List.of("abstracted work"), CALLS);
    }

    @Test
    void aProducedAbstractClassIsBoundByItsBindings() {
        assertEquals("work", context.getBean(BoundAbstractClass.class).work());

        assertEquals(List.of("abstracted work"), CALLS);
    }
}
