package io.micronaut.interceptor.test.noreflection;

import io.micronaut.core.annotation.RegisterAnnotations;

/**
 * Has Micronaut generate the builder of {@link Labelled} as the application is compiled.
 */
@RegisterAnnotations(Labelled.class)
final class Builders {

    private Builders() {
    }
}
