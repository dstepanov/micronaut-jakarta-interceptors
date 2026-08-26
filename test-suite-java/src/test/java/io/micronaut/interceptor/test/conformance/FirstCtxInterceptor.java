package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Ctx
@Priority(Interceptor.Priority.APPLICATION - 10)
public class FirstCtxInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("first context " + System.identityHashCode(context));
        Calls.RECORDED.add("first thread " + Thread.currentThread().getName());
        Calls.RECORDED.add("first sees " + context.getContextData().get("fromEarlier"));
        context.getContextData().put("fromFirst", "written by the first");
        return context.proceed();
    }
}
