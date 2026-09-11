package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;

/**
 * Fails the first time it is invoked, and succeeds from then on.
 */
@Singleton
@Retrying
@LaterMicronautAdvice
public class RetriedService {

    private int attempts;

    public String attempt() {
        Log.RECORDED.add("target");
        if (attempts++ == 0) {
            throw new IllegalStateException("the first attempt fails");
        }
        return "succeeded";
    }
}
