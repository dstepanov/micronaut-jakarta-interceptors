package io.micronaut.interceptor.test

import jakarta.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
@Bracketed
open class ResumingService {

    open suspend fun suspends(): String {
        Calls.recorded += "body start"
        delay(20)
        Calls.recorded += "body resumed"
        return "done"
    }

    open suspend fun failsAfterResuming(): String {
        Calls.recorded += "body start"
        delay(20)
        Calls.recorded += "body resumed"
        throw IllegalStateException("after resuming")
    }

    @Suppress("RedundantSuspendModifier")
    open suspend fun neverSuspends(): String {
        Calls.recorded += "body"
        return "done"
    }
}
