package io.micronaut.interceptor.internal.runtime;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.metadata.InterceptionKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The chains of an element and the interceptor classes associated with its construction are kept in one map, under
 * the interception the processor recorded, and only the kind tells the two apart.
 */
class RecordedKeyTest {

    private static AnnotationValue<JakartaInterception> interception() {
        return AnnotationValue.builder(JakartaInterception.class)
            .member(JakartaInterception.ASSOCIATED, new AnnotationClassValue<>(String.class))
            .build();
    }

    @Test
    void theKeyOfAnAssociationIsNotTheKeyOfAnyChainOfTheSameInterception() {
        RecordedKey associated = RecordedKey.of(null, interception());
        for (InterceptionKind kind : InterceptionKind.values()) {
            assertNotEquals(RecordedKey.of(kind, interception()), associated, kind.toString());
        }
    }

    @Test
    void theSameInterceptionIsTheSameKey() {
        assertEquals(RecordedKey.of(null, interception()), RecordedKey.of(null, interception()));
        assertEquals(RecordedKey.of(InterceptionKind.AROUND_CONSTRUCT, interception()),
            RecordedKey.of(InterceptionKind.AROUND_CONSTRUCT, interception()));
    }
}
