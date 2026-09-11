package io.micronaut.interceptor.test.state;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps what it learns as the object it intercepts is created, and reads it back as that object is invoked, which
 * only works if one instance does both.
 */
@Interceptor
@Paired
public class PairedInterceptor {

    /** The instances that interposed on the post-construct event of an object, in order. */
    public static final List<PairedInterceptor> POST_CONSTRUCTED = Collections.synchronizedList(new ArrayList<>());

    /** The instances that interposed on a business method, in order. */
    public static final List<PairedInterceptor> INVOKED = Collections.synchronizedList(new ArrayList<>());

    /** The instances that were destroyed, in order. */
    public static final List<PairedInterceptor> DESTROYED = Collections.synchronizedList(new ArrayList<>());

    private Object created;

    public static void clear() {
        POST_CONSTRUCTED.clear();
        INVOKED.clear();
        DESTROYED.clear();
    }

    @PostConstruct
    public void created(InvocationContext context) throws Exception {
        created = context.getTarget();
        POST_CONSTRUCTED.add(this);
        context.proceed();
    }

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        INVOKED.add(this);
        return context.proceed();
    }

    /** The object this instance saw created, or {@code null} when it saw none. */
    public Object created() {
        return created;
    }

    @PreDestroy
    void close() {
        DESTROYED.add(this);
    }
}
