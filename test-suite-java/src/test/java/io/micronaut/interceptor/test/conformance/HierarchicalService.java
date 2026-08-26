package io.micronaut.interceptor.test.conformance;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

@Singleton
@Hierarchical
public class HierarchicalService extends BaseHierarchicalService {

    public String work() {
        Calls.RECORDED.add("target");
        return "done";
    }

    @AroundInvoke
    Object ownInterceptorMethod(InvocationContext context) throws Exception {
        Calls.RECORDED.add("target own");
        return context.proceed();
    }
}
