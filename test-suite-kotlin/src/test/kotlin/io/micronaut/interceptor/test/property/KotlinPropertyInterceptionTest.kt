package io.micronaut.interceptor.test.property

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A Kotlin property accessor is an ordinary method of the class on the virtual machine, and the specification
 * intercepts it as it intercepts any other non-private, non-static method: a binding on the accessor itself, and a
 * binding on the class it belongs to, both reach it.
 */
class KotlinPropertyInterceptionTest {

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
    fun clear() = Calls.clear()

    @Test
    fun interceptsAGetterThatCarriesTheBinding() {
        assertEquals("read", context.getBean(AccessorBoundService::class.java).read)
        assertEquals(listOf("guarded getRead", "read"), Calls.recorded)
    }

    @Test
    fun interceptsASetterThatCarriesTheBinding() {
        val service = context.getBean(AccessorBoundService::class.java)
        service.written = "other"
        assertEquals("other", service.written)
        assertEquals(listOf("guarded setWritten", "set written", "guarded getWritten", "get written"), Calls.recorded)
    }

    @Test
    fun leavesAnAccessorThatCarriesNoBindingAlone() {
        assertEquals("plain", context.getBean(AccessorBoundService::class.java).plain)
        assertEquals(listOf("plain"), Calls.recorded)
    }

    @Test
    fun compilesAnOrdinaryOpenClassThatAlsoDeclaresAPropertyWithoutOpen() {
        val service = context.getBean(PartlyOpenService::class.java)
        assertEquals("work", service.work())
        assertEquals("fixed", service.fixed)
        // the property is not open, so there is nothing to intercept it through, and the class still compiles
        assertEquals(listOf("guarded work", "work", "fixed"), Calls.recorded)
    }

    @Test
    fun interceptsTheAccessorsOfAClassBoundAtClassLevel() {
        val service = context.getBean(ClassBoundService::class.java)
        assertEquals("read", service.read)
        assertEquals("work", service.work())
        service.written = "other"
        assertEquals(
            listOf("guarded getRead", "read", "guarded work", "work", "guarded setWritten", "set written"),
            Calls.recorded
        )
    }
}
