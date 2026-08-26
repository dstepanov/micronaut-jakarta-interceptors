package io.micronaut.interceptor.test.edge;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@MemberBound
public class MemberBoundInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Log.RECORDED.add("memberBound saw " + context.getParameters()[0]);
        context.setParameters(new Object[]{"replaced"});
        return context.proceed();
    }
}
