package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PostConstruct;

@Prototype
@Unready
public class UnreadyService {

    @PostConstruct
    void start() {
        throw new IllegalStateException("never ready");
    }
}
