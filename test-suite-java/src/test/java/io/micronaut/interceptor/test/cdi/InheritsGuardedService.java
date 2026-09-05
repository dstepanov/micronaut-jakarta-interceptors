package io.micronaut.interceptor.test.cdi;

import jakarta.inject.Singleton;

/** Inherits the bound method as it is. */
@Singleton
public class InheritsGuardedService extends GuardedBase {
}
