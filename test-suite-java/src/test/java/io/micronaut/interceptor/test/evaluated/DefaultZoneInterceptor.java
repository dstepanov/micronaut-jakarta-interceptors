package io.micronaut.interceptor.test.evaluated;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.lang.annotation.Annotation;

/** Bound to the default value of {@link Zone}, which is what the intercepted method declares. */
@Interceptor
@Zone
public class DefaultZoneInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        Calls.RECORDED.add("default zone interceptor");
        Zone one = context.getInterceptorBinding(Zone.class);
        Calls.RECORDED.add("getInterceptorBinding " + (one == null ? "nothing" : one.value()));
        for (Zone of : context.getInterceptorBindings(Zone.class)) {
            Calls.RECORDED.add("getInterceptorBindings(Zone) " + of.value());
        }
        for (Annotation binding : context.getInterceptorBindings()) {
            if (binding instanceof Zone zone) {
                Calls.RECORDED.add("getInterceptorBindings " + zone.value());
            }
        }
        return context.proceed();
    }
}
