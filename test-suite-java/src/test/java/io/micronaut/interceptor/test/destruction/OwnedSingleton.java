package io.micronaut.interceptor.test.destruction;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
@Owned
public class OwnedSingleton {

    public String work() {
        return "done";
    }

    @PreDestroy
    void close() {
        Destructions.RECORDED.add("target destroyed");
    }
}
