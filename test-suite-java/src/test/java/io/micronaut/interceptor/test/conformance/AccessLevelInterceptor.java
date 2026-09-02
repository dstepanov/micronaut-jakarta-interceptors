package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Sections 2.6 cc)/cd) and 2.7 jc)/jd): an interceptor method may have protected or package level access. One
 * method may also interpose on more than one lifecycle event, which section 2.7 f) allows, and a value it returns
 * is ignored, which section 2.7 ia) requires.
 */
@Interceptor
@Levels
public class AccessLevelInterceptor {

    @AroundInvoke
    protected Object protectedAround(InvocationContext context) throws Exception {
        Calls.RECORDED.add("protected aroundInvoke");
        return context.proceed();
    }

    @PostConstruct
    @PreDestroy
    Object bothCallbacks(InvocationContext context) throws Exception {
        Calls.RECORDED.add("package private callback");
        context.proceed();
        return "this return value is ignored";
    }
}
