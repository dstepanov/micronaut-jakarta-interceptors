package io.micronaut.interceptor.test.edge;

import jakarta.inject.Singleton;

/**
 * A bean intercepted both by a Jakarta Interceptors binding and by a native Micronaut advice.
 */
@Singleton
@Alpha
@MicronautAdvice
public class MixedService {

    public String work() {
        Log.RECORDED.add("target");
        return "done";
    }
}
