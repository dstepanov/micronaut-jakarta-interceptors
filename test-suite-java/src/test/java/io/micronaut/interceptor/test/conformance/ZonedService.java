package io.micronaut.interceptor.test.conformance;

import io.micronaut.context.annotation.Prototype;

/**
 * Sections 3.3 bc) and da): the constructor declares two bindings, one of which replaces the binding of the same
 * type declared at class level.
 */
@Prototype
@Zone("a")
public class ZonedService {

    @Zone("b")
    @Audited2
    public ZonedService() {
    }
}
