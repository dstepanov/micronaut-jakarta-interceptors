package io.micronaut.interceptor.test.shutdown.early;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A singleton interceptor that interposes on the destruction of what it is bound to and holds a resource of its own.
 *
 * <p>It lives in a package that sorts before the package of the bean it intercepts: Micronaut destroys the singletons
 * of a closing context in the order of the names of their definitions wherever nothing makes one of them depend on
 * another, so this is the one that would go first.</p>
 */
@Singleton
@Interceptor
@Guarded
public class AGuardingInterceptor {

    public static final List<String> RECORDED = Collections.synchronizedList(new ArrayList<>());

    private boolean closed;

    public static void reset() {
        RECORDED.clear();
    }

    /**
     * Interposes on the destruction of the intercepted bean, which the specification has happen while this instance
     * is still usable.
     */
    @PreDestroy
    public void beforeDestroy(InvocationContext context) throws Exception {
        RECORDED.add(closed ? "intercepted a destruction after being closed" : "intercepted a destruction");
        context.proceed();
    }

    /**
     * The destruction callback of the interceptor itself, which takes no {@code InvocationContext} and so is not an
     * interceptor method.
     */
    @PreDestroy
    void close() {
        closed = true;
        RECORDED.add("interceptor closed");
    }
}
