
package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PreDestroy;

/**
 * Injected into the singleton interceptor, and so destroyed only with the interceptor: when the context closes.
 */
@Prototype
public class SharedResource {

    @PreDestroy
    void release() {
        Destruction.CALLS.add("resource of the singleton interceptor destroyed");
    }
}
