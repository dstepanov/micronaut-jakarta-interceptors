package io.micronaut.interceptor.test.errors;

import jakarta.inject.Singleton;

@Singleton
@Guarded
public class GuardedService {

    public static boolean ran;

    public String checked() throws RefusedException {
        ran = true;
        return "ok";
    }

    public String failing() {
        ran = true;
        throw new IllegalStateException("from the target");
    }
}
