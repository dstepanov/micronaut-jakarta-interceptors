package io.micronaut.interceptor.test.binding

import jakarta.inject.Singleton
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

final class ShelfCalls {

    static final List<String> RECORDED = []

    private ShelfCalls() {
    }
}

@Interceptor
@Shelved
class UnnamedShelfInterceptor {

    @AroundInvoke
    Object intercept(InvocationContext context) throws Exception {
        ShelfCalls.RECORDED << "unnamed shelf"
        return context.proceed()
    }
}

@Singleton
class ShelvedService {

    @Shelved
    String leftToItsDefaults() {
        return "defaults"
    }

    @Shelved("")
    String emptyValueSpeltOut() {
        return "empty value"
    }

    @Shelved("top")
    String onANamedShelf() {
        return "named"
    }
}
