package io.micronaut.interceptor.test.hierarchy;

import io.micronaut.interceptor.test.hierarchy.remote.RemoteBaseInterceptor;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Remote
public class RemoteInterceptor extends RemoteBaseInterceptor {

    @AroundInvoke
    Object ownAround(InvocationContext context) throws Exception {
        Hierarchy.CALLS.add("own around");
        return context.proceed();
    }
}
