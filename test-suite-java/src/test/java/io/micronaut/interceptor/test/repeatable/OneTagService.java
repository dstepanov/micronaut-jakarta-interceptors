package io.micronaut.interceptor.test.repeatable;

import jakarta.inject.Singleton;

@Singleton
@Tag("one")
public class OneTagService {

    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
