package io.micronaut.interceptor.test.edge;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Beta
public class BetaInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Class<?>[] parameters = context.getMethod().getParameterTypes();
        Log.RECORDED.add("Beta on "
            + (parameters.length == 0 ? "nothing" : parameters[0].getSimpleName()));
        return context.proceed();
    }
}
