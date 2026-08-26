package io.micronaut.interceptor.test.concurrency;

import jakarta.annotation.PostConstruct;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records what it sees of each invocation, so that a test may check that concurrent invocations do not see each
 * other's context, parameters or result.
 */
@Interceptor
@Concurrent
public class IsolationInterceptor {

    static final Set<Object> INSTANCES = ConcurrentHashMap.newKeySet();
    static final AtomicInteger CONSTRUCTED = new AtomicInteger();
    static final AtomicInteger CREATED = new AtomicInteger();
    static final Set<String> LEAKED = ConcurrentHashMap.newKeySet();

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        INSTANCES.add(this);
        String mine = String.valueOf(context.getParameters()[0]);

        // the context data belongs to this invocation alone
        if (context.getContextData().put("token", mine) != null) {
            LEAKED.add("context data of another invocation was already present: " + mine);
        }
        Object result = context.proceed();

        Object readBack = context.getContextData().get("token");
        if (!mine.equals(readBack)) {
            LEAKED.add("context data changed under the invocation: wrote " + mine + " read " + readBack);
        }
        if (!("echo " + mine).equals(result)) {
            LEAKED.add("result of another invocation: expected echo " + mine + " got " + result);
        }
        if (!mine.equals(String.valueOf(context.getParameters()[0]))) {
            LEAKED.add("parameters changed under the invocation: " + mine);
        }
        return result;
    }

    @AroundConstruct
    public void construct(InvocationContext context) throws Exception {
        CONSTRUCTED.incrementAndGet();
        context.proceed();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        CREATED.incrementAndGet();
        context.proceed();
    }
}
