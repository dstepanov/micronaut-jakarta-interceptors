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

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface OfTheConstruction {
    }

    @Interceptor
    @OfTheConstruction
    public static class ConstructionInterceptor {
        @jakarta.interceptor.AroundConstruct
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("around construct");
            return context.proceed();
        }
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

    public static class SelfInterceptingProduct {
        public String work() {
            CALLS.add("work");
            return "work";
        }

        @AroundInvoke
        Object self(InvocationContext context) throws Exception {
            CALLS.add("product self");
            return context.proceed();
        }
    }

    @Factory
    public static class SelfInterceptingProductFactory {
        @Singleton
        @OfTheMember
        SelfInterceptingProduct product() {
            return new SelfInterceptingProduct();
        }

        @AroundInvoke
        Object self(InvocationContext context) throws Exception {
            CALLS.add("factory self");
            return context.proceed();
        }
    }

    /**
     * Interposes on the construction of another object, which makes it an interceptor class, named by
     * {@code @Interceptors} somewhere, although it does not declare {@code @Interceptor}.
     */
    public static class LifecycleInterposing {
        public String work() {
            CALLS.add("work");
            return "work";
        }

        @jakarta.annotation.PostConstruct
        private void constructed(InvocationContext context) throws Exception {
            CALLS.add("constructed");
            context.proceed();
        }
    }

    @Factory
    public static class LifecycleInterposingFactory {
        @Singleton
        @OfTheMember
        @OfTheConstruction
        LifecycleInterposing lifecycleInterposing() {
            return new LifecycleInterposing();
        }
    }

    @Test
    void aProducedInterceptorClassThatDoesNotSaySoIsNotIntercepted() {
        // the factory reads @Interceptor from the type alone, not its private interceptor methods; what the type
        // records of them is what leaves the bean it produces unintercepted, its construction included
        assertEquals("work", context.getBean(LifecycleInterposing.class).work());

        assertEquals(List.of("work"), CALLS);
    }

    @Test
    void theProducerMethodOfAnInterceptedFactoryDoesNotRunTheInterceptorMethodOfTheProduct() {
        // the factory interposes on itself, so Micronaut intercepts producing the bean as well, by what the member
        // binds; the interceptor method of the product is a method of the product, and the target is the factory
        context.getBean(SelfInterceptingProduct.class).work();

        assertEquals(List.of("member binding", "member binding", "product self", "work"), CALLS);
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
