package io.micronaut.interceptor.test.variations;

import jakarta.inject.Singleton;

@Singleton
@Inheriting
public class InheritingService {

    public String work() {
        Calls.RECORDED.add("target");
        return "done";
    }
}
