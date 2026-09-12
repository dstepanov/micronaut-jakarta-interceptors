package io.micronaut.interceptor.test.repeatable;

import jakarta.inject.Singleton;

@Singleton
@Tag("two")
@Tag("one")
public class BothTagsService {

    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
