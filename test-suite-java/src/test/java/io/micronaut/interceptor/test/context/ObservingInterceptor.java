package io.micronaut.interceptor.test.context;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;

@Interceptor
@Observed
@Priority(Interceptor.Priority.APPLICATION - 1)
public class ObservingInterceptor {

    @AroundInvoke
    public Object observe(InvocationContext context) throws Exception {
        Observation.record(context);
        Observation.contextData = new ArrayList<>();
        context.getContextData().put("greeting", "hello");
        Object result = context.proceed();
        Observation.contextData.add(String.valueOf(context.getContextData().get("answer")));
        return result;
    }
}
