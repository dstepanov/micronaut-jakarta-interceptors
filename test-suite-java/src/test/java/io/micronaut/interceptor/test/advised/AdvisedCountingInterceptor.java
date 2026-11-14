package io.micronaut.interceptor.test.advised;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An interceptor class Micronaut proxies, because one of its methods carries Micronaut advice of its own: the
 * definition describing it is of the subclass Micronaut generates.
 */
@Interceptor
@Counted
@Priority(Interceptor.Priority.APPLICATION)
public class AdvisedCountingInterceptor {

    public static final List<String> RECORDED = new CopyOnWriteArrayList<>();

    @AroundInvoke
    Object invoke(InvocationContext context) throws Exception {
        RECORDED.add("counted " + context.getMethod().getName());
        return context.proceed();
    }

    @MicronautAdvised
    public String describe() {
        return "advised";
    }
}
