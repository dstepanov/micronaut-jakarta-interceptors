package io.micronaut.interceptor.test

import io.micronaut.context.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SuspendInterceptionTest {

    /**
     * A suspending function is intercepted like any other: the arguments it declares are the ones the interceptor
     * sees and may replace, and the continuation the Kotlin compiler adds after them is not shown.
     */
    @Test
    fun interceptsASuspendingFunction() {
        ApplicationContext.run().use { context ->
            Calls.clear()

            val result = runBlocking { context.getBean(SuspendingService::class.java).suspending("Denis") }

            // the continuation the Kotlin compiler adds is not one of the parameters, and replacing the one
            // parameter the function declares reaches it
            assertEquals("Hello replaced", result)
            assertEquals(listOf("parameters [Denis]", "body"), Calls.recorded)
        }
    }

    /**
     * The interception is synchronous, and a suspending function returns from its call the first time it
     * suspends. `proceed()` hands the interceptor the marker that says so, the interceptor returns and its
     * `finally` runs, and only then does the function resume and complete. The caller still receives the value
     * the function completes with.
     */
    @Test
    fun theInterceptorReturnsWhenTheFunctionFirstSuspendsRatherThanWhenItCompletes() {
        assertEquals(
            listOf(
                "interceptor before",
                "body start",
                "interceptor returned COROUTINE_SUSPENDED",
                "interceptor finished",
                "body resumed",
                "caller received done"),
            recorded { it.suspends() })
    }

    /**
     * An exception thrown once the function has resumed is thrown after the interceptor has returned, so the
     * interceptor cannot catch it. It reaches the caller.
     */
    @Test
    fun anExceptionThrownAfterResumingReachesTheCallerButNotTheInterceptor() {
        assertEquals(
            listOf(
                "interceptor before",
                "body start",
                "interceptor returned COROUTINE_SUSPENDED",
                "interceptor finished",
                "body resumed",
                "caller caught after resuming"),
            recorded { it.failsAfterResuming() })
    }

    /**
     * A suspending function that completes without suspending returns its value from the call, so an interceptor
     * sees it through to completion as it would an ordinary function.
     */
    @Test
    fun aFunctionThatDoesNotSuspendIsInterceptedToItsCompletion() {
        assertEquals(
            listOf(
                "interceptor before",
                "body",
                "interceptor returned done",
                "interceptor finished",
                "caller received done"),
            recorded { it.neverSuspends() })
    }

    private fun recorded(call: suspend (ResumingService) -> String): List<String> =
        ApplicationContext.run().use { context ->
            Calls.clear()
            val service = context.getBean(ResumingService::class.java)
            try {
                val result = runBlocking { call(service) }
                Calls.recorded += "caller received $result"
            } catch (e: IllegalStateException) {
                Calls.recorded += "caller caught ${e.message}"
            }
            Calls.recorded.toList()
        }
}
