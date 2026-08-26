package io.micronaut.interceptor.test

import jakarta.inject.Singleton
import jakarta.interceptor.Interceptors

@Singleton
@Logged
open class GreetingService {

    open fun greet(name: String): String {
        Calls.recorded += "greet"
        return "Hello $name"
    }

    @Interceptors(CountingInterceptor::class)
    open fun counted(): String {
        Calls.recorded += "counted"
        return "counted"
    }
}
