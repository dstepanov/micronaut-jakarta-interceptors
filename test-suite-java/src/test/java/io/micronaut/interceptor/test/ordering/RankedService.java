package io.micronaut.interceptor.test.ordering;

import jakarta.inject.Singleton;

@Singleton
public class RankedService {

    @Ranked
    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
