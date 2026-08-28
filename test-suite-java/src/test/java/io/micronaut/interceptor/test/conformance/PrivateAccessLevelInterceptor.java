package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Sections 2.6 cb) and 2.7 jb): an interceptor method may have private access. A private method has no executable
 * method to call it directly, so it is reached reflectively instead.
 */
@Interceptor
@PrivateLevels
public class PrivateAccessLevelInterceptor {

    @AroundInvoke
    private Object privateAround(InvocationContext context) throws Exception {
        Calls.RECORDED.add("private aroundInvoke");
        return context.proceed();
    }

    @PostConstruct
    private void privateCallback(InvocationContext context) throws Exception {
        Calls.RECORDED.add("private callback");
        context.proceed();
    }
}
