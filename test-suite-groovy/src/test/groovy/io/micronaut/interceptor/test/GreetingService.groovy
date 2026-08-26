package io.micronaut.interceptor.test

import jakarta.inject.Singleton
import jakarta.interceptor.Interceptors

@Singleton
@Logged
class GreetingService {

    String greet(String name) {
        Calls.RECORDED << "greet"
        return "Hello $name"
    }

    @Interceptors(CountingInterceptor)
    String counted() {
        Calls.RECORDED << "counted"
        return "counted"
    }
}
