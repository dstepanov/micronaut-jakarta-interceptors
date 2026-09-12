package io.micronaut.interceptor.test.repeatable;

import jakarta.inject.Singleton;

@Singleton
@Noted(value = "one", note = "first")
@Noted(value = "two", note = "second")
public class NotedService {

    public String work() {
        Calls.RECORDED.add("work");
        return "done";
    }
}
