package io.micronaut.interceptor.test.variations;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

@Singleton
@Everything
public class EveryKindService {

    @PostConstruct
    void start() {
        Calls.RECORDED.add("target postConstruct");
    }

    public String work() {
        Calls.RECORDED.add("target work");
        return "done";
    }
}
