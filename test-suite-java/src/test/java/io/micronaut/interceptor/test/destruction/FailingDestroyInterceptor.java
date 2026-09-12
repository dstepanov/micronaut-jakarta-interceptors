package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.LifeCycle;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * An interceptor whose destruction fails. Created last of the two interceptors of {@link FailingService}, so that it
 * is the first the reverse order of destruction reaches.
 */
@Interceptor
@Failing
@Priority(Interceptor.Priority.APPLICATION + 10)
public class FailingDestroyInterceptor implements LifeCycle<FailingDestroyInterceptor> {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public FailingDestroyInterceptor stop() {
        Destructions.RECORDED.add("failing interceptor refused to be destroyed");
        throw new IllegalStateException("this interceptor cannot be destroyed");
    }
}
