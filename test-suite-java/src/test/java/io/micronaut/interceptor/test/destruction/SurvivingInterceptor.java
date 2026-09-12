package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/** The other interceptor of {@link FailingService}, destroyed after the one whose destruction fails. */
@Interceptor
@Failing
@Priority(Interceptor.Priority.APPLICATION - 10)
public class SurvivingInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        return context.proceed();
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("surviving interceptor destroyed");
    }
}
