
package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PreDestroy;

/**
 * Injected into the interceptor. An interceptor class cannot declare a pre-destroy callback of its own - one it
 * declares is an interceptor method - so its destruction is observed through what was injected into it, which is
 * destroyed with it.
 */
@Prototype
public class ReleasedResource {

    @PreDestroy
    void release() {
        Destruction.CALLS.add("resource of the interceptor destroyed");
    }
}
