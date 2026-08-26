package io.micronaut.interceptor.test.injection;

import jakarta.inject.Singleton;

@Singleton
@Audited
public class AuditedService {

    public String save() {
        return "saved";
    }
}
