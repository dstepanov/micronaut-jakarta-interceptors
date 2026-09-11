package io.micronaut.interceptor.test.errors;

import io.micronaut.context.annotation.Prototype;

@Prototype
@PrivatelyGuarded
public class PrivatelyGuardedService {

    public String checked() throws RefusedException {
        return "ok";
    }

    public String failing() {
        throw new IllegalStateException("from the target");
    }
}
