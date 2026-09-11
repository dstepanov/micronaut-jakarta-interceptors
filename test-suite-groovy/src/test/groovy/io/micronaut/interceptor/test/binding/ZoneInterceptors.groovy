package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

final class ZoneCalls {

    static final List<String> RECORDED = []

    private ZoneCalls() {
    }
}

@Interceptor
@Zone("a")
class ZoneAInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        ZoneCalls.RECORDED << "zone a"
        return context.proceed()
    }
}

@Interceptor
@Zone
class DefaultZoneInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        ZoneCalls.RECORDED << "zone z"
        return context.proceed()
    }
}

@Singleton
@Zone("a")
class ZonedService {

    String inherited() {
        return "inherited"
    }

    /**
     * Replaces the class's {@code @Zone("a")} with {@code @Zone("z")}, the default of the member.
     */
    @Zone
    String replacedByDefault() {
        return "by default"
    }
}
