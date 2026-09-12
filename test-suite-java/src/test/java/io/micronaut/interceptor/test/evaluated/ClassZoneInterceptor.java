package io.micronaut.interceptor.test.evaluated;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** Bound to the value the class declares, which the method replaces, so it interposes on the class's other methods. */
@Interceptor
@Zone("class")
public class ClassZoneInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("class zone interceptor");
        return context.proceed();
    }
}
