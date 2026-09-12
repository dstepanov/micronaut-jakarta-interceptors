package io.micronaut.interceptor.test.destruction;

import io.micronaut.context.annotation.Prototype;

/** A bean whose pre-destroy event an ordinary Micronaut interceptor keeps the Jakarta chain from seeing. */
@Prototype
@Suppressed
@SuppressedDestroyAdvice
public class SuppressedService {

    public String work() {
        return "done";
    }
}
