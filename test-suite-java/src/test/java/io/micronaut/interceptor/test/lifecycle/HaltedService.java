package io.micronaut.interceptor.test.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
@Halted
public class HaltedService {

    @PostConstruct
    void started() {
        Calls.RECORDED.add("callback postConstruct");
    }

    @PreDestroy
    void stopped() {
        Calls.RECORDED.add("callback preDestroy");
    }
}
