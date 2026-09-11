package io.micronaut.interceptor.test.factory;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class AuditingInterceptorFactory {

    @Singleton
    AuditingInterceptor auditingInterceptor() {
        return new AuditingInterceptor("factory");
    }
}
