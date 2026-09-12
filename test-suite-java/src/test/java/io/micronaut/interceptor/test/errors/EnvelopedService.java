package io.micronaut.interceptor.test.errors;

import io.micronaut.context.annotation.Prototype;

@Prototype
@Enveloped
public class EnvelopedService {

    public String work() {
        return "ok";
    }
}
