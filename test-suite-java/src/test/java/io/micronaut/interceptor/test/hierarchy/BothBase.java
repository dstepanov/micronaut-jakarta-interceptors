package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class BothBase {

    @PostConstruct
    void initBase() {
        Hierarchy.CALLS.add("base post");
    }

    @PreDestroy
    void destroyBase() {
        Hierarchy.CALLS.add("base pre");
    }
}
