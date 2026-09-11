package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

final class WatchCalls {

    static final List<String> RECORDED = []

    private WatchCalls() {
    }
}

@Interceptor
@Watched
class WatchingInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        WatchCalls.RECORDED << "watched ${context.method.name}".toString()
        return context.proceed()
    }
}

/**
 * The plain carrier on a method, which is all the interception the class declares.
 */
@Singleton
class SurveilledService {

    @Surveilled
    String work() {
        return "work"
    }

    String rest() {
        return "rest"
    }
}
