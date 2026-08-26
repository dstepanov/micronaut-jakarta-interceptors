package io.micronaut.interceptor.test.context;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@Observed
@Priority(Interceptor.Priority.APPLICATION)
public class RewritingInterceptor {

    @AroundInvoke
    public Object rewrite(InvocationContext context) throws Exception {
        // the context data is shared with the interceptors before and after this one
        context.getContextData().put("answer", context.getContextData().get("greeting") + " again");
        Object[] parameters = context.getParameters();
        if (parameters.length == 1 && parameters[0] instanceof String name) {
            context.setParameters(new Object[]{name.toUpperCase()});
        }
        return context.proceed() + "!";
    }
}
