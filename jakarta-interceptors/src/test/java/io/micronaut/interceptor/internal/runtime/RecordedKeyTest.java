package io.micronaut.interceptor.internal.runtime;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.metadata.InterceptionKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The chains of every kind are kept in one map, under the interception the processor recorded, and only the kind
 * tells the chains of one interception apart.
 */
class RecordedKeyTest {

    private static AnnotationValue<JakartaInterception> interception() {
        return AnnotationValue.builder(JakartaInterception.class)
            .member(JakartaInterception.INTERCEPTORS, new AnnotationClassValue<>(String.class))
            .build();
    }

    @Test
    void theKeysOfTwoKindsOfTheSameInterceptionDiffer() {
        for (InterceptionKind kind : InterceptionKind.values()) {
            for (InterceptionKind other : InterceptionKind.values()) {
                if (kind != other) {
                    assertNotEquals(RecordedKey.of(kind, interception()), RecordedKey.of(other, interception()),
                        kind + " " + other);
                }
            }
        }
    }

    @Test
    void theKeysOfEveryKindAreTheKeysOfEachKind() {
        RecordedKey[] keys = RecordedKey.ofEveryKind(interception());
        for (InterceptionKind kind : InterceptionKind.values()) {
            RecordedKey key = RecordedKey.of(kind, interception());
            assertEquals(key, keys[kind.ordinal()], kind.toString());
            assertEquals(key.hashCode(), keys[kind.ordinal()].hashCode(), kind.toString());
        }
        assertEquals(RecordedKey.of(InterceptionKind.AROUND_INVOKE, null),
            RecordedKey.ofEveryKind(null)[InterceptionKind.AROUND_INVOKE.ordinal()]);
    }

    @Test
    void theSameInterceptionIsTheSameKey() {
        assertEquals(RecordedKey.of(InterceptionKind.AROUND_CONSTRUCT, interception()),
            RecordedKey.of(InterceptionKind.AROUND_CONSTRUCT, interception()));
    }
}
