package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PreDestroy;

/** Something an interceptor instance holds of its own, which goes when the interceptor does. */
@Prototype
public class InterceptorResource {

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("interceptor resource destroyed");
    }
}
