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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Where interception and interceptor classes meet in one hierarchy. An interceptor class extending an intercepted
 * class is not intercepted by what it inherits, although it is bound by an inherited binding. A bean extending an
 * interceptor class without declaring {@code @Interceptor} is a target class, intercepted by the interceptor method it
 * inherits, and not a second interceptor class. A binding that is not inherited - one declared on an interface, or
 * one declared on a superclass without {@code @Inherited} - does not bind the class that implements or extends it,
 * while a binding declared on a method it inherits does.
 */
class InterceptorClassHierarchyTest {

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
    @interface Marked {
    }

    @Inherited
    @InterceptorBinding
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @interface Lineage {
    }

    @Interceptor
    @Marked
    public static class MarkingInterceptor {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("marked " + context.getMethod().getName());
            return context.proceed();
        }

        public String utility() {
            return "utility";
        }
    }

    @Lineage
    public static class LineageBase {
        public String inherited() {
            return "inherited";
        }

        protected String guardedByTheClass() {
            return "protected";
        }

        @Marked
        public String marked() {
            return "marked";
        }
    }

    @Interceptor
    public static class LineageInterceptor extends LineageBase {
        @AroundInvoke
        public Object around(InvocationContext context) throws Exception {
            CALLS.add("lineage " + context.getMethod().getName());
            return context.proceed();
        }

        public String callProtected() {
            return guardedByTheClass();
        }
    }

    @Singleton
    @Lineage
    public static class LineageService {
        public String work() {
            return "work";
        }
    }

    @Singleton
    public static class MarkingSubclass extends MarkingInterceptor {
        public String work() {
            return "work";
        }
    }

    @Singleton
    @Marked
    public static class MarkedService {
        public String work() {
            return "work";
        }
    }

    @Marked
    public abstract static class AbstractMarked {
        public String work() {
            return "work";
        }

        @Lineage
        public String bound() {
            return "bound";
        }
    }

    @Singleton
    public static class ConcreteMarked extends AbstractMarked {
    }

    @Marked
    public interface MarkedApi {
        String work();

        @Marked
        String bound();
    }

    @Singleton
    public static class MarkedApiImplementation implements MarkedApi {
        @Override
        public String work() {
            return "work";
        }

        @Override
        public String bound() {
            return "bound";
        }
    }

    @Test
    void anInterceptorClassIsNotInterceptedByWhatItInheritsFromAnInterceptedClass() {
        LineageInterceptor interceptor = context.getBean(LineageInterceptor.class);

        assertSame(LineageInterceptor.class, interceptor.getClass());
        assertEquals("inherited", interceptor.inherited());
        assertEquals("protected", interceptor.callProtected());
        assertEquals("marked", interceptor.marked());
        assertEquals(List.of(), CALLS);
    }

    @Test
    void anInterceptorClassIsBoundByTheBindingItInherits() {
        assertEquals("work", context.getBean(LineageService.class).work());

        assertEquals(List.of("lineage work"), CALLS);
    }

    @Test
    void aBeanExtendingAnInterceptorClassInterceptsItselfWithTheMethodItInherits() {
        MarkingSubclass bean = context.getBean(MarkingSubclass.class);

        assertEquals("work", bean.work());
        assertEquals(List.of("marked work"), CALLS);
    }

    /**
     * The bean is of the type of the interceptor class and takes its place where the interceptor class is looked up
     * by type, since the interceptor class is a secondary bean; the interceptor of the binding is still an instance
     * of the interceptor class, invoked once.
     */
    @Test
    void aBeanExtendingAnInterceptorClassIsNotASecondInterceptorClass() {
        assertEquals("work", context.getBean(MarkedService.class).work());

        assertEquals(List.of("marked work"), CALLS);
    }

    @Test
    void aBindingOfAnAbstractSuperclassThatIsNotInheritedDoesNotBindTheSubclass() {
        assertEquals("work", context.getBean(ConcreteMarked.class).work());

        assertEquals(List.of(), CALLS);
    }

    @Test
    void aBindingOnAMethodTheSubclassInheritsBindsIt() {
        assertEquals("bound", context.getBean(ConcreteMarked.class).bound());

        assertEquals(List.of("lineage bound"), CALLS);
    }

    @Test
    void theBindingsOfAnInterfaceDoNotBindItsImplementation() {
        MarkedApiImplementation bean = context.getBean(MarkedApiImplementation.class);

        assertSame(MarkedApiImplementation.class, bean.getClass());
        assertEquals("work", bean.work());
        assertEquals("bound", bean.bound());
        assertEquals(List.of(), CALLS);
    }
}
