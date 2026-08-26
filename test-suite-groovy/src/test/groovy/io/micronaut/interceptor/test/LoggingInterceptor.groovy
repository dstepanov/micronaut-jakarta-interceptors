package io.micronaut.interceptor.test

import jakarta.annotation.Priority
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@Interceptor
@Logged
@Priority(Interceptor.Priority.APPLICATION - 10)
class LoggingInterceptor {

    @AroundInvoke
    Object log(InvocationContext context) throws Exception {
        Calls.RECORDED << "log in ${context.method.name}".toString()
        try {
            return context.proceed()
        } finally {
            Calls.RECORDED << "log out"
        }
    }
}
