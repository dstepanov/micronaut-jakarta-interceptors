package io.micronaut.interceptor.test.lifecycle;

import jakarta.inject.Singleton;

/**
 * Intercepted for its lifecycle, but declaring no callback of its own: there is no method for the specification to
 * show a lifecycle callback interceptor method.
 */
@Singleton
@Managed
public class CallbacklessService {

    public String work() {
        return "done";
    }
}
