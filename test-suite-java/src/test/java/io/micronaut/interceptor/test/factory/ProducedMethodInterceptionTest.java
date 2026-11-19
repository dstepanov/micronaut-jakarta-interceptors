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
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.Interceptors;
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
 * What a method of a produced type declares on itself is combined with what the factory member declares, as it is
 * with what a class declares for a bean of that class: a binding of the method is added to those of the member, and
 * the interceptor classes the method names come after those the member names.
 */
class ProducedMethodInterceptionTest {

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

    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface OfTheMethod {
    }

    @Interceptor
    @OfTheMember
    @Priority(10)
    public static class MemberBindingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("member binding");
            return context.proceed();
        }
    }

    @Interceptor
    @OfTheMethod
    @Priority(20)
    public static class MethodBindingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("method binding");
            return context.proceed();
        }
    }

    public static class NamedByTheMember {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("named by the member");
            return context.proceed();
        }
    }

    public static class NamedByTheMethod {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("named by the method");
            return context.proceed();
        }
    }

    public static class Product {
        @OfTheMethod
        @Interceptors(NamedByTheMethod.class)
        public String bound() {
            CALLS.add("bound");
            return "bound";
        }

        public String unbound() {
            CALLS.add("unbound");
            return "unbound";
        }

        @ExcludeClassInterceptors
        public String excluding() {
            CALLS.add("excluding");
            return "excluding";
        }
    }

    public record Book(String title) {
    }

    /**
     * A generic type whose methods Micronaut compiles with the parameter types of the produced type that extends it:
     * {@code save(Book)} of a {@code BookRepository}, where this class declares {@code save(Object)}.
     */
    public static class Repository<T> {
        @OfTheMethod
        @Interceptors(NamedByTheMethod.class)
        public T save(T item) {
            CALLS.add("save");
            return item;
        }

        public T find(T item) {
            CALLS.add("find");
            return item;
        }

        @ExcludeClassInterceptors
        public T delete(T item) {
            CALLS.add("delete");
            return item;
        }
    }

    public static class BookRepository extends Repository<Book> {
    }

    /**
     * A generic type produced with a type argument, whose methods Micronaut compiles as this class declares them.
     */
    public static class Box<T> {
        @OfTheMethod
        public T put(T item) {
            CALLS.add("put");
            return item;
        }
    }

    @Factory
    public static class ProductFactory {
        @Singleton
        @OfTheMember
        @Interceptors(NamedByTheMember.class)
        Product product() {
            return new Product();
        }

        @Singleton
        @OfTheMember
        @Interceptors(NamedByTheMember.class)
        BookRepository books() {
            return new BookRepository();
        }

        @Singleton
        @OfTheMember
        Box<Book> box() {
            return new Box<>();
        }
    }

    @Test
    void aBindingOfTheMethodIsCombinedWithTheOneOfTheMember() {
        context.getBean(Product.class).bound();

        assertEquals(List.of("named by the member", "named by the method", "member binding", "method binding",
            "bound"), CALLS);
    }

    @Test
    void aMethodDeclaringNothingIsInterceptedByWhatTheMemberDeclares() {
        context.getBean(Product.class).unbound();

        assertEquals(List.of("named by the member", "member binding", "unbound"), CALLS);
    }

    @Test
    void aMethodExcludingTheClassInterceptorsExcludesTheOnesTheMemberNames() {
        context.getBean(Product.class).excluding();

        assertEquals(List.of("member binding", "excluding"), CALLS);
    }

    @Test
    void aMethodOfAGenericSuperclassKeepsItsBindingAndInterceptorClasses() {
        context.getBean(BookRepository.class).save(new Book("Dune"));

        assertEquals(List.of("named by the member", "named by the method", "member binding", "method binding",
            "save"), CALLS);
    }

    @Test
    void aMethodOfAGenericSuperclassDeclaringNothingIsInterceptedByWhatTheMemberDeclares() {
        context.getBean(BookRepository.class).find(new Book("Dune"));

        assertEquals(List.of("named by the member", "member binding", "find"), CALLS);
    }

    @Test
    void aMethodOfAGenericSuperclassExcludingTheClassInterceptorsExcludesTheOnesTheMemberNames() {
        context.getBean(BookRepository.class).delete(new Book("Dune"));

        assertEquals(List.of("member binding", "delete"), CALLS);
    }

    @Test
    void aMethodOfAGenericTypeProducedWithATypeArgumentKeepsItsBinding() {
        context.getBean(Box.class).put(new Book("Dune"));

        assertEquals(List.of("member binding", "method binding", "put"), CALLS);
    }
}
