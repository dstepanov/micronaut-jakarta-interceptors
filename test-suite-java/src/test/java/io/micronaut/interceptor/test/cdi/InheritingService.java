package io.micronaut.interceptor.test.cdi;

import jakarta.inject.Singleton;

/**
 * Declares no binding of its own: the one of its superclass is {@code @Inherited}.
 */
@Singleton
public class InheritingService extends BaseService {
}
