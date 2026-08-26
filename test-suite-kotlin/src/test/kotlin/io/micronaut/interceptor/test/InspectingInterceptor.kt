package io.micronaut.interceptor.test

import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

@Interceptor
@Inspected
open class InspectingInterceptor {

    @AroundInvoke
    open fun inspect(context: InvocationContext): Any? {
        Calls.recorded += "parameters ${context.parameters.toList()}"
        context.setParameters(arrayOf("replaced"))
        return context.proceed()
    }
}
