package io.micronaut.interceptor.test.hierarchy;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
@Armed
public class Both extends BothBase {

    @PostConstruct
    void initOwn() {
        Hierarchy.CALLS.add("own post");
    }

    @PreDestroy
    void destroyOwn() {
        Hierarchy.CALLS.add("own pre");
    }
}
