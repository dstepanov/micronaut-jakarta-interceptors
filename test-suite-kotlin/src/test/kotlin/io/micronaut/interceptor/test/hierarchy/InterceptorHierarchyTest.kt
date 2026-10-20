package io.micronaut.interceptor.test.hierarchy

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InterceptorBinding
import jakarta.interceptor.InvocationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.annotation.Inherited

@Inherited
@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Lineage

@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Marked

object HierarchyCalls {

    val recorded: MutableList<String> = mutableListOf()
}

@Interceptor
@Marked
open class MarkingInterceptor {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        HierarchyCalls.recorded += "marked ${context.method.name}"
        return context.proceed()
    }

    open fun utility(): String {
        HierarchyCalls.recorded += "utility"
        return "utility"
    }
}

/** A class bound at class level by an inherited binding, and on one of its functions by another binding. */
@Lineage
open class LineageBase {

    open fun inherited(): String = "inherited"

    @Marked
    open fun marked(): String = "marked"
}

/** An interceptor class that inherits its binding, and the bound functions, of the class it extends. */
@Interceptor
open class LineageInterceptor : LineageBase() {

    @AroundInvoke
    open fun intercept(context: InvocationContext): Any? {
        HierarchyCalls.recorded += "lineage ${context.method.name}"
        return context.proceed()
    }
}

@Singleton
@Lineage
open class LineageService {

    open fun work(): String = "work"
}

/** A bean extending an interceptor class without being one: the interceptor method it inherits is its own. */
@Singleton
open class MarkingSubclass : MarkingInterceptor() {

    open fun work(): String = "work"
}

@Singleton
@Marked
open class MarkedService {

    open fun work(): String = "work"
}

open class SelfIntercepting {

    open fun work(): String = "work"

    @AroundInvoke
    open fun self(context: InvocationContext): Any? {
        HierarchyCalls.recorded += "self ${context.method.name}"
        return context.proceed()
    }
}

@Factory
open class SelfInterceptingFactory {

    @Singleton
    @Marked
    open fun selfIntercepting(): SelfIntercepting = SelfIntercepting()
}

/**
 * What an interceptor class inherits from the classes it extends, and what a class inherits from an interceptor
 * class it extends, as it is in Java. An interceptor class is not intercepted by the bound functions of its
 * superclass, and is bound by the inherited binding of it; a bean extending an interceptor class intercepts itself
 * with the interceptor method it inherits, and is not taken for a second interceptor class.
 */
class InterceptorHierarchyTest {

    companion object {

        private lateinit var context: ApplicationContext

        @JvmStatic
        @BeforeAll
        fun startContext() {
            context = ApplicationContext.run()
        }

        @JvmStatic
        @AfterAll
        fun stopContext() {
            context.close()
        }
    }

    @BeforeEach
    fun clear() = HierarchyCalls.recorded.clear()

    @Test
    fun anInterceptorClassIsNotInterceptedByTheBindingsOfItsSuperclass() {
        val interceptor = context.getBean(LineageInterceptor::class.java)

        assertEquals(LineageInterceptor::class.java, interceptor.javaClass)
        assertEquals("inherited", interceptor.inherited())
        assertEquals("marked", interceptor.marked())
        assertEquals(emptyList<String>(), HierarchyCalls.recorded)
    }

    @Test
    fun anInterceptorClassIsBoundByTheBindingItInherits() {
        assertEquals("work", context.getBean(LineageService::class.java).work())

        assertEquals(listOf("lineage work"), HierarchyCalls.recorded)
    }

    @Test
    fun aBeanExtendingAnInterceptorClassInterceptsItself() {
        assertEquals("work", context.getBean(MarkingSubclass::class.java).work())

        assertEquals(listOf("marked work"), HierarchyCalls.recorded)
    }

    @Test
    fun aBeanExtendingAnInterceptorClassIsNotASecondInterceptorClass() {
        assertEquals("work", context.getBean(MarkedService::class.java).work())

        assertEquals(listOf("marked work"), HierarchyCalls.recorded)
    }

    @Test
    fun aBeanAFactoryMemberBindsInterceptsItself() {
        assertEquals("work", context.getBean(SelfIntercepting::class.java).work())

        assertEquals(listOf("marked work", "self work"), HierarchyCalls.recorded)
    }
}
