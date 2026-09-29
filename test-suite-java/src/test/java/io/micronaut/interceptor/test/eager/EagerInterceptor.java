package io.micronaut.interceptor.test.eager;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.*;

@Interceptor
@Eager
public class EagerInterceptor {
    public static final List<String> CREATED = Collections.synchronizedList(new ArrayList<>());
    public static final List<EagerInterceptor> INVOKED = Collections.synchronizedList(new ArrayList<>());
    public EagerInterceptor() {
        CREATED.add("created " + System.identityHashCode(this));
    }
    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        INVOKED.add(this);
        return context.proceed();
    }
}
