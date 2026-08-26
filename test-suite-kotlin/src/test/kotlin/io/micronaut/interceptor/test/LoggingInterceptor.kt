package io.micronaut.interceptor.test

import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@Interceptor
@Logged
@Priority(Interceptor.Priority.APPLICATION - 10)
open class LoggingInterceptor {

    @AroundInvoke
    open fun log(context: InvocationContext): Any? {
        Calls.recorded += "log in ${context.method.name}"
        try {
            return context.proceed()
        } finally {
            Calls.recorded += "log out"
        }
    }
}
