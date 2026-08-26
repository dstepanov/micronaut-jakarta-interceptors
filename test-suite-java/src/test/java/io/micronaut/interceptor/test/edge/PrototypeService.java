package io.micronaut.interceptor.test.edge;

import io.micronaut.context.annotation.Prototype;
import jakarta.annotation.PostConstruct;

@Prototype
@Alpha
public class PrototypeService {

    @PostConstruct
    void start() {
    }

    public String work() {
        return "done";
    }
}
