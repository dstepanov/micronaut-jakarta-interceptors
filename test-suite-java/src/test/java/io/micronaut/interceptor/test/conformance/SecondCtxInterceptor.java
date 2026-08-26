package io.micronaut.interceptor.test.conformance;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Set;

@Interceptor
@Ctx
@Priority(Interceptor.Priority.APPLICATION)
public class SecondCtxInterceptor {

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        Calls.RECORDED.add("second context " + System.identityHashCode(context));
        Calls.RECORDED.add("second thread " + Thread.currentThread().getName());
        Calls.RECORDED.add("second sees " + context.getContextData().get("fromFirst"));
        context.getContextData().put("fromEarlier", "written during this invocation");

        Set<java.lang.annotation.Annotation> bindings = context.getInterceptorBindings();
        Calls.RECORDED.add("bindings " + bindings.stream()
            .map(a -> a.annotationType().getSimpleName()).sorted().toList());
        Unbound unbound = context.getInterceptorBinding(Unbound.class);
        Calls.RECORDED.add("unbound label " + (unbound == null ? "absent" : unbound.label()));

        if (context.getParameters().length == 1) {
            context.setParameters(new Object[]{"replaced"});
            Calls.RECORDED.add("after set " + context.getParameters()[0]);
            try {
                context.setParameters(new Object[]{"a", "b"});
            } catch (IllegalArgumentException e) {
                Calls.RECORDED.add("wrong count rejected");
            }
            try {
                context.setParameters(new Object[]{42});
            } catch (IllegalArgumentException e) {
                Calls.RECORDED.add("wrong type rejected");
            }
        }
        return context.proceed();
    }
}
