package io.micronaut.interceptor.test.brokencases;

import io.micronaut.interceptor.test.state.Paired;
import io.micronaut.runtime.context.scope.ThreadLocal;

@ThreadLocal(lifecycle = true)
@Paired
public class ScopedTargetService {
    public String work() {
        return "done";
    }
}
