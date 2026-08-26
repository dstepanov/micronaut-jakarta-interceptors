package io.micronaut.interceptor.test.concurrency;

import jakarta.inject.Singleton;

@Singleton
@Concurrent
public class ConcurrentSingleton {

    public String echo(String value) {
        return "echo " + value;
    }
}
