package io.micronaut.interceptor.test.graalvm;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Traced("service")
public class TracingInterceptor {

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundConstruct");
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        Calls.RECORDED.add("postConstruct");
        context.proceed();
    }

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("aroundInvoke " + context.getParameters()[0]);
        context.setParameters(new Object[]{"replaced"});
        return context.proceed();
    }
}
