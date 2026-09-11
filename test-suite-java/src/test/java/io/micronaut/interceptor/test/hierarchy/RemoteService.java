package io.micronaut.interceptor.test.hierarchy;

import jakarta.inject.Singleton;

@Singleton
@Remote
public class RemoteService {

    public String work() {
        Hierarchy.CALLS.add("work");
        return "worked";
    }
}
