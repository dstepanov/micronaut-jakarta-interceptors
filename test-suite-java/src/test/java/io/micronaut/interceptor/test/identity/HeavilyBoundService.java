package io.micronaut.interceptor.test.identity;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

/**
 * A bean that declares everything the module intercepts through at once: two binding annotations, an interceptor
 * class named directly, an interceptor method of its own, and a method that declares a binding again.
 */
@Singleton
@Audited
@Logged
@Interceptors(NamedInterceptor.class)
public class HeavilyBoundService {

    public String work() {
        return "done";
    }

    @Audited
    public String boundAgain() {
        return "again";
    }

    @AroundInvoke
    Object itsOwn(InvocationContext context) throws Exception {
        return context.proceed();
    }
}
