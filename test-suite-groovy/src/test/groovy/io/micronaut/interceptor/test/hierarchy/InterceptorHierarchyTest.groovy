package io.micronaut.interceptor.test.hierarchy

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * What an interceptor class inherits from the classes it extends, and what a class inherits from an interceptor
 * class it extends, as it is in Java. An interceptor class is not intercepted by the bound methods of its
 * superclass, and is bound by the inherited binding of it; a bean extending an interceptor class intercepts itself
 * with the interceptor method it inherits, and is not taken for a second interceptor class.
 */
class InterceptorHierarchyTest {

    static final List<String> CALLS = []

    private static ApplicationContext context

    @BeforeAll
    static void startContext() {
        context = ApplicationContext.run()
    }

    @AfterAll
    static void stopContext() {
        context.close()
    }

    @BeforeEach
    void clear() {
        CALLS.clear()
    }

    @Test
    void anInterceptorClassIsNotInterceptedByTheBindingsOfItsSuperclass() {
        LineageInterceptor interceptor = context.getBean(LineageInterceptor)

        assertEquals(LineageInterceptor, interceptor.getClass())
        assertEquals("inherited", interceptor.inherited())
        assertEquals("marked", interceptor.marked())
        assertEquals([], CALLS)
    }

    @Test
    void anInterceptorClassIsBoundByTheBindingItInherits() {
        assertEquals("work", context.getBean(LineageService).work())

        assertEquals(["lineage work"], CALLS)
    }

    @Test
    void aBeanExtendingAnInterceptorClassInterceptsItself() {
        assertEquals("work", context.getBean(MarkingSubclass).work())

        assertEquals(["marked work"], CALLS)
    }

    @Test
    void aBeanExtendingAnInterceptorClassIsNotASecondInterceptorClass() {
        assertEquals("work", context.getBean(MarkedService).work())

        assertEquals(["marked work"], CALLS)
    }

    @Test
    void aBeanAFactoryMemberBindsInterceptsItself() {
        assertEquals("work", context.getBean(SelfIntercepting).work())

        assertEquals(["marked work", "self work"], CALLS)
    }
}

@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Lineage {
}

@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface Marked {
}

@Interceptor
@Marked
class MarkingInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        InterceptorHierarchyTest.CALLS << "marked ${context.method.name}".toString()
        return context.proceed()
    }

    String utility() {
        InterceptorHierarchyTest.CALLS << "utility"
        return "utility"
    }
}

/** A class bound at class level by an inherited binding, and on one of its methods by another binding. */
@Lineage
class LineageBase {

    String inherited() {
        return "inherited"
    }

    @Marked
    String marked() {
        return "marked"
    }
}

/** An interceptor class that inherits its binding, and the bound methods, of the class it extends. */
@Interceptor
class LineageInterceptor extends LineageBase {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        InterceptorHierarchyTest.CALLS << "lineage ${context.method.name}".toString()
        return context.proceed()
    }
}

@Singleton
@Lineage
class LineageService {

    String work() {
        return "work"
    }
}

/** A bean extending an interceptor class without being one: the interceptor method it inherits is its own. */
@Singleton
class MarkingSubclass extends MarkingInterceptor {

    String work() {
        return "work"
    }
}

@Singleton
@Marked
class MarkedService {

    String work() {
        return "work"
    }
}

class SelfIntercepting {

    String work() {
        return "work"
    }

    @AroundInvoke
    Object self(InvocationContext context) throws Exception {
        InterceptorHierarchyTest.CALLS << "self ${context.method.name}".toString()
        return context.proceed()
    }
}

@Factory
class SelfInterceptingFactory {

    @Singleton
    @Marked
    SelfIntercepting selfIntercepting() {
        return new SelfIntercepting()
    }
}
