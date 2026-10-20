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
 * A bean a factory produces is intercepted by the interceptor methods its class declares on itself, as a bean of
 * that class declared directly is, whether the interception comes from the bindings of the class or from the ones
 * the factory member declares. The interceptor methods are invoked through the definition generated from the class:
 * the proxy Micronaut generates for the produced bean carries what the class records, but exposes the methods it
 * advises rather than the interceptor methods.
 */
class ProducedSelfInterceptionTest {

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
    @interface OfTheClass {
    }

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface OfTheMember {
    }

    @Interceptor
    @OfTheClass
    public static class ClassBindingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("class binding");
            return context.proceed();
        }
    }

    @Interceptor
    @OfTheMember
    public static class MemberBindingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("member binding");
            return context.proceed();
        }
    }

    @OfTheClass
    public static class BoundSelfIntercepting {
        public String work() {
            CALLS.add("work");
            return "work";
        }

        @AroundInvoke
        Object self(InvocationContext context) throws Exception {
            CALLS.add("self");
            return context.proceed();
        }
    }

    public static class SelfIntercepting {
        public String work() {
            CALLS.add("work");
            return "work";
        }

        @AroundInvoke
        Object self(InvocationContext context) throws Exception {
            CALLS.add("self");
            return context.proceed();
        }
    }

    @Factory
    public static class SelfInterceptingFactory {
        @Singleton
        @Named("asTheClassDeclares")
        BoundSelfIntercepting asTheClassDeclares() {
            return new BoundSelfIntercepting();
        }

        @Singleton
        @Named("boundByTheMember")
        @OfTheMember
        BoundSelfIntercepting boundByTheMember() {
            return new BoundSelfIntercepting();
        }

        @Singleton
        @OfTheMember
        SelfIntercepting selfIntercepting() {
            return new SelfIntercepting();
        }
    }

    @Test
    void theClassInterceptsItselfWhereTheFactoryDeclaresNothing() {
        context.getBean(BoundSelfIntercepting.class, Qualifiers.byName("asTheClassDeclares")).work();

        assertEquals(List.of("class binding", "self", "work"), CALLS);
    }

    @Test
    void theClassInterceptsItselfWhereTheMemberDeclaresABinding() {
        context.getBean(BoundSelfIntercepting.class, Qualifiers.byName("boundByTheMember")).work();

        assertEquals(List.of("class binding", "member binding", "self", "work"), CALLS);
    }

    @Test
    void aClassBoundByTheMemberAloneInterceptsItself() {
        context.getBean(SelfIntercepting.class).work();

        assertEquals(List.of("member binding", "self", "work"), CALLS);
    }
}
