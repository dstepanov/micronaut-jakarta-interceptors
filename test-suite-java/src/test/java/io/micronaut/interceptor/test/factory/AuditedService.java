package io.micronaut.interceptor.test.factory;

import jakarta.inject.Singleton;

@Singleton
public class AuditedService {

    @Audited
    public String work() {
        return "work";
    }
}
