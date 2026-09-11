package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Doomed
public class DoomedService {

    public DoomedService() {
        throw new IllegalStateException("never constructed");
    }
}
