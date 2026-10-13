package io.micronaut.interceptor.spi;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One binding compared with another as {@link InterceptorBindings} compares them, for a module that compares them one
 * at a time, as Micronaut CDI does for its qualifiers and its bean manager.
 */
class BindingValuesTest {

    private static final String REGION = "test.Region";

    private static AnnotationValue<?> value(Map<CharSequence, Object> members, Map<CharSequence, Object> defaults) {
        return new AnnotationValue<>(REGION, members, defaults);
    }

    @Test
    void comparesAsTheBindingsOfAnInterceptorAreCompared() {
        AnnotationValue<?> declared = value(Map.of("value", "eu", "unit", TimeUnit.SECONDS), Map.of());
        AnnotationValue<?> recorded = value(Map.of("value", "eu", "unit", "SECONDS"), Map.of());
        AnnotationValue<?> other = value(Map.of("value", "us", "unit", "SECONDS"), Map.of());

        assertTrue(BindingValues.matches(declared, recorded));
        assertFalse(BindingValues.matches(declared, other));
        assertEquals(InterceptorBindings.ofValues(List.of(declared)).isBoundBy(InterceptorBindings.ofValues(List.of(recorded))),
            BindingValues.matches(declared, recorded));
        assertEquals(BindingValues.hashCode(declared), BindingValues.hashCode(recorded));
    }

    @Test
    void fillsInTheDefaultsTheValueCarries() {
        AnnotationValue<?> defaulted = value(Map.of(), Map.of("value", "eu"));

        assertTrue(BindingValues.matches(defaulted, value(Map.of("value", "eu"), Map.of())));
        assertEquals("eu", BindingValues.normalize(defaulted).stringValue().orElse(null));
    }

    @Test
    void anEmptyStringIsTheMemberLeftOutOnceTheDefaultsAreFilledIn() {
        assertTrue(BindingValues.matches(value(Map.of("value", ""), Map.of()), value(Map.of(), Map.of())));
        assertFalse(BindingValues.matches(value(Map.of("value", ""), Map.of("value", "eu")),
            value(Map.of(), Map.of("value", "eu"))));
    }

    @Test
    void leavesOutTheMembersEitherSideExcludes() {
        AnnotationValue<?> excluding = value(Map.of("value", "eu", "note", "a",
            AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"note"}), Map.of());

        assertTrue(BindingValues.matches(excluding, value(Map.of("value", "eu", "note", "b"), Map.of())));
        assertTrue(BindingValues.matches(value(Map.of("value", "eu", "note", "b"), Map.of()), excluding));
        AnnotationValue<?> normalized = BindingValues.normalize(excluding);
        assertFalse(normalized.contains("note"));
        assertArrayEquals(new String[]{"note"}, normalized.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE));
    }

    @Test
    void aMemberNamedWithADollarBindsUnlessMicronautReservesIt() {
        AnnotationValue<?> withStereotypes = value(Map.of("value", "eu",
            AnnotationUtil.STEREOTYPES_MEMBER, new AnnotationValue<?>[0]), Map.of());

        assertTrue(BindingValues.matches(withStereotypes, value(Map.of("value", "eu"), Map.of())));
        assertFalse(BindingValues.matches(value(Map.of("value", "eu", "$zone", "a"), Map.of()),
            value(Map.of("value", "eu", "$zone", "b"), Map.of())));
    }

    @Test
    void normalizingANormalizedValueGivesItAgain() {
        AnnotationValue<?> declared = value(Map.of("value", "eu", "type", String.class), Map.of("unit", TimeUnit.SECONDS));
        AnnotationValue<?> normalized = BindingValues.normalize(declared);

        assertEquals(new AnnotationClassValue<>(String.class.getName()), normalized.getValues().get("type"));
        assertEquals("SECONDS", normalized.getValues().get("unit"));
        assertTrue(BindingValues.matches(normalized, BindingValues.normalize(normalized)));
        assertEquals(normalized.getValues(), BindingValues.normalize(normalized).getValues());
    }
}
