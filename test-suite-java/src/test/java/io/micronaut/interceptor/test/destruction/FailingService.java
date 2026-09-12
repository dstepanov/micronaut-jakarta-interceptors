package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Failing
public class FailingService {

    public String work() {
        return "done";
    }
}
