package io.micronaut.interceptor.test.repeatable;

import jakarta.inject.Singleton;

/** Bound by two occurrences of the repeatable binding the method declares, through their container. */
@Singleton
public class MethodTagsService {

    @Tag("two")
    @Tag("one")
    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
