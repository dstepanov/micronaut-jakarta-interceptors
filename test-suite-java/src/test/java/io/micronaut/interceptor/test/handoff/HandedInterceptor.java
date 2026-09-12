package io.micronaut.interceptor.test.handoff;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Records every instance of itself, so that a second set created for one object is visible. */
@Interceptor
@Handed
public class HandedInterceptor {

    /** Every instance ever constructed, in order. */
    public static final List<HandedInterceptor> CREATED = Collections.synchronizedList(new ArrayList<>());

    /** The instances that interposed on a business method, in order. */
    public static final List<HandedInterceptor> INVOKED = Collections.synchronizedList(new ArrayList<>());

    /** The instances that were destroyed, in order. */
    public static final List<HandedInterceptor> DESTROYED = Collections.synchronizedList(new ArrayList<>());

    public HandedInterceptor() {
        CREATED.add(this);
    }

    public static void clear() {
        CREATED.clear();
        INVOKED.clear();
        DESTROYED.clear();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        context.proceed();
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        INVOKED.add(this);
        return context.proceed();
    }

    @PreDestroy
    void close() {
        DESTROYED.add(this);
    }
}
