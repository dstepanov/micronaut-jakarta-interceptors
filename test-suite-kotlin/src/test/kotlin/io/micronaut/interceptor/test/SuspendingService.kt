package io.micronaut.interceptor.test

import jakarta.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
@Inspected
open class SuspendingService {

    open suspend fun suspending(name: String): String {
        Calls.recorded += "body"
        delay(1)
        return "Hello $name"
    }
}
