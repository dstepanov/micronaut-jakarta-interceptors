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
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.interceptor.test.library.BoundLibraryService;
import io.micronaut.interceptor.test.library.LibraryBinding;
import io.micronaut.interceptor.test.library.MethodBoundLibraryService;
import jakarta.annotation.Priority;
import jakarta.inject.Named;
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
 * A bean a factory produces is intercepted by what its type declares although the type was compiled apart from the
 * factory, in a library: what the processor wrote on the type while compiling it is not in its class file, so the
 * interception of the bean is what the processor of the factory writes, at class level and for each method.
 */
class ProducedTypeCompiledApartTest {

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
    @interface OfTheMember {
    }

    @Interceptor
    @LibraryBinding
    @Priority(10)
    public static class LibraryBindingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("library binding");
            return context.proceed();
        }
    }

    @Interceptor
    @OfTheMember
    @Priority(20)
    public static class MemberBindingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("member binding");
            return context.proceed();
        }
    }

    @Factory
    public static class LibraryFactory {
        @Singleton
        @Named("asTheClassDeclares")
        BoundLibraryService asTheClassDeclares() {
            return new BoundLibraryService();
        }

        @Singleton
        @Named("boundByTheMember")
        @OfTheMember
        BoundLibraryService boundByTheMember() {
            return new BoundLibraryService();
        }

        @Singleton
        MethodBoundLibraryService methodBound() {
            return new MethodBoundLibraryService();
        }
    }

    @Test
    void aClassLevelBindingOfTheTypeInterceptsWhereTheFactoryDeclaresNothing() {
        context.getBean(BoundLibraryService.class, Qualifiers.byName("asTheClassDeclares")).work(CALLS);

        assertEquals(List.of("library binding", "work"), CALLS);
    }

    @Test
    void aClassLevelBindingOfTheTypeIsCombinedWithTheOneOfTheMember() {
        context.getBean(BoundLibraryService.class, Qualifiers.byName("boundByTheMember")).work(CALLS);

        assertEquals(List.of("library binding", "member binding", "work"), CALLS);
    }

    @Test
    void aMethodOfObjectIsNotInterceptedByTheClassLevelBinding() {
        String described = context.getBean(BoundLibraryService.class, Qualifiers.byName("asTheClassDeclares"))
            .toString();

        assertEquals("bound library service", described);
        assertEquals(List.of(), CALLS);
    }

    @Test
    void aMethodLevelBindingOfTheTypeInterceptsThatMethodAlone() {
        MethodBoundLibraryService service = context.getBean(MethodBoundLibraryService.class);

        service.bound(CALLS);
        service.unbound(CALLS);

        assertEquals(List.of("library binding", "bound", "unbound"), CALLS);
    }
}
