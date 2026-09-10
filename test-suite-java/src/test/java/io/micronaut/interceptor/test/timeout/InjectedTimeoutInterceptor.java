package io.micronaut.interceptor.test.timeout;

import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Section 2.8 d): an around-timeout interceptor may take injection and use what it was given. */
@Interceptor
@Varied
public class InjectedTimeoutInterceptor {

    private final TimeoutCollaborator collaborator;

    public InjectedTimeoutInterceptor(TimeoutCollaborator collaborator) {
        this.collaborator = collaborator;
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext context) throws Exception {
        collaborator.consulted();
        return context.proceed();
    }
}
