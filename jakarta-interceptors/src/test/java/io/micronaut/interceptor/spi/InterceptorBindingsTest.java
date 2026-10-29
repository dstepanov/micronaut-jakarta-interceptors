package io.micronaut.interceptor.spi;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interceptor is bound to an interception point when every binding of the interceptor is a binding of the point,
 * compared by the members that bind.
 */
class InterceptorBindingsTest {

    private static final String LOGGED = "test.Logged";
    private static final String AUDITED = "test.Audited";
    private static final String TAG = "test.Tag";

    private static AnnotationValue<?> value(String name, Map<CharSequence, Object> members) {
        return AnnotationValue.builder(name).members(members).build();
    }

    private static InterceptorBindings bindings(AnnotationValue<?>... values) {
        return InterceptorBindings.ofValues(List.of(values));
    }

    @Test
    void everyBindingOfTheInterceptorIsOneOfThePoint() {
        AnnotationValue<?> logged = value(LOGGED, Map.of());
        AnnotationValue<?> audited = value(AUDITED, Map.of());

        assertTrue(bindings(logged).isBoundBy(bindings(logged, audited)));
        assertTrue(bindings(logged, audited).isBoundBy(bindings(audited, logged)));
        assertFalse(bindings(logged, audited).isBoundBy(bindings(logged)));
        assertFalse(bindings(logged).isBoundBy(bindings()));
    }

    @Test
    void anInterceptorWithoutBindingsIsBoundToNothing() {
        assertFalse(bindings().isBoundBy(bindings(value(LOGGED, Map.of()))));
        assertTrue(bindings().isEmpty());
    }

    @Test
    void theMembersAreCompared() {
        assertFalse(bindings(value(LOGGED, Map.of("level", "INFO")))
            .isBoundBy(bindings(value(LOGGED, Map.of("level", "DEBUG")))));
    }

    @Test
    void eachOccurrenceOfARepeatableBindingIsABinding() {
        InterceptorBindings point = bindings(value(TAG, Map.of("value", "one")), value(TAG, Map.of("value", "two")));

        assertTrue(bindings(value(TAG, Map.of("value", "one"))).isBoundBy(point));
        assertTrue(bindings(value(TAG, Map.of("value", "two")), value(TAG, Map.of("value", "one"))).isBoundBy(point));
        assertFalse(bindings(value(TAG, Map.of("value", "one")), value(TAG, Map.of("value", "three"))).isBoundBy(point));
    }

    /**
     * The processor lists the members the annotation type excludes on what it records; a value built from an
     * annotation instance carries every member and may list none of them.
     */
    @Test
    void aMemberExcludedOnEitherSideIsLeftOutOfBoth() {
        AnnotationValue<?> recorded = AnnotationValue.builder(LOGGED)
            .member("level", "INFO")
            .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"comment"})
            .build();
        AnnotationValue<?> instance = value(LOGGED, Map.of("level", "INFO", "comment", "anything"));

        assertTrue(bindings(recorded).isBoundBy(bindings(instance)));
        assertTrue(bindings(instance).isBoundBy(bindings(recorded)));
        assertFalse(bindings(recorded).isBoundBy(bindings(value(LOGGED, Map.of("level", "DEBUG", "comment", "x")))));
    }

    @Test
    void theDefaultsAreFilledIn() {
        AnnotationValue<?> defaulted = AnnotationValue.builder(LOGGED).defaultValues(Map.of("level", "INFO")).build();

        assertTrue(bindings(defaulted).isBoundBy(bindings(value(LOGGED, Map.of("level", "INFO")))));
        assertTrue(bindings(value(LOGGED, Map.of("level", "INFO"))).isBoundBy(bindings(defaulted)));
        assertFalse(bindings(defaulted).isBoundBy(bindings(value(LOGGED, Map.of("level", "DEBUG")))));
    }

    /**
     * Micronaut records no default of an empty string for an annotation compiled from Java or Kotlin, so an empty
     * string without a default reads as the member left out.
     */
    @Test
    void anEmptyStringWithoutADefaultIsTheMemberLeftOut() {
        assertTrue(bindings(value(LOGGED, Map.of("value", ""))).isBoundBy(bindings(value(LOGGED, Map.of()))));
        assertTrue(bindings(value(LOGGED, Map.of())).isBoundBy(bindings(value(LOGGED, Map.of("value", "")))));
    }

    @Test
    void aClassAndAnEnumConstantCompareAsRecorded() {
        assertTrue(bindings(value(LOGGED, Map.of("type", String.class)))
            .isBoundBy(bindings(value(LOGGED, Map.of("type", new AnnotationClassValue<>("java.lang.String"))))));
        assertTrue(bindings(value(LOGGED, Map.of("unit", TimeUnit.SECONDS)))
            .isBoundBy(bindings(value(LOGGED, Map.of("unit", "SECONDS")))));
        assertTrue(bindings(value(LOGGED, Map.of("units", new TimeUnit[]{TimeUnit.SECONDS})))
            .isBoundBy(bindings(value(LOGGED, Map.of("units", new String[]{"SECONDS"})))));
    }

    @Test
    void anArrayComparesByItsElements() {
        assertTrue(bindings(value(LOGGED, Map.of("value", new int[]{1, 2})))
            .isBoundBy(bindings(value(LOGGED, Map.of("value", new int[]{1, 2})))));
        assertFalse(bindings(value(LOGGED, Map.of("value", new int[]{1, 2})))
            .isBoundBy(bindings(value(LOGGED, Map.of("value", new int[]{2, 1})))));
        assertFalse(bindings(value(LOGGED, Map.of("value", new String[0])))
            .isBoundBy(bindings(value(LOGGED, Map.of("value", new String[]{""})))));
    }

    /**
     * An array the bindings were read from stays the caller's: changed afterwards, it changes neither what the
     * bindings are compared by nor the values they hand out.
     */
    @Test
    void anArrayTheBindingsWereReadFromStaysTheCallers() {
        String[] tags = {"a"};
        InterceptorBindings bindings = bindings(value(LOGGED, Map.of("value", tags)));

        tags[0] = "b";

        assertTrue(bindings.isBoundBy(bindings(value(LOGGED, Map.of("value", new String[]{"a"})))));
        assertEquals(List.of("a"), List.of(bindings.values().get(0).stringValues("value")));
    }

    /**
     * An annotation held by a member compares by every member of it, the ones it defaults to included: an
     * annotation value's own {@code equals} would pass over a member the other one does not declare.
     */
    @Test
    void anAnnotationHeldByAMemberComparesByEveryMember() {
        AnnotationValue<?> defaulted = AnnotationValue.builder("test.Inner")
            .defaultValues(Map.of("value", "a", "other", "b"))
            .build();
        AnnotationValue<?> spelledOut = value("test.Inner", Map.of("value", "a", "other", "b"));
        AnnotationValue<?> partial = value("test.Inner", Map.of("value", "a"));
        AnnotationValue<?> different = value("test.Inner", Map.of("value", "a", "other", "c"));

        assertTrue(bindings(value(LOGGED, Map.of("inner", new AnnotationValue<?>[]{defaulted})))
            .isBoundBy(bindings(value(LOGGED, Map.of("inner", new AnnotationValue<?>[]{spelledOut})))));
        assertFalse(bindings(value(LOGGED, Map.of("inner", new AnnotationValue<?>[]{partial})))
            .isBoundBy(bindings(value(LOGGED, Map.of("inner", new AnnotationValue<?>[]{spelledOut})))));
        assertFalse(bindings(value(LOGGED, Map.of("inner", new AnnotationValue<?>[]{different})))
            .isBoundBy(bindings(value(LOGGED, Map.of("inner", new AnnotationValue<?>[]{spelledOut})))));
    }

    @Test
    void equalBindingsAreEqualWhateverTheirOrderAndSpelling() {
        InterceptorBindings one = bindings(value(LOGGED, Map.of("level", "INFO")), value(AUDITED, Map.of()));
        InterceptorBindings other = bindings(value(AUDITED, Map.of()),
            AnnotationValue.builder(LOGGED).defaultValues(Map.of("level", "INFO")).build());
        InterceptorBindings different = bindings(value(LOGGED, Map.of("level", "DEBUG")), value(AUDITED, Map.of()));

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
        assertNotEquals(one, different);
        assertEquals(bindings(value(TAG, Map.of("value", "one"))),
            bindings(value(TAG, Map.of("value", "one")), value(TAG, Map.of("value", "one"))));
    }

    /**
     * The values read back are the bindings again, spelt as the processor records them, with the excluded members
     * listed rather than held, and a binding declared twice there once.
     */
    @Test
    void theValuesAreTheBindingsSpeltAsRecorded() {
        AnnotationValue<?> logged = AnnotationValue.builder(LOGGED)
            .member("unit", TimeUnit.SECONDS)
            .member("comment", "anything")
            .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"comment"})
            .defaultValues(Map.of("level", "INFO"))
            .build();
        InterceptorBindings bindings = bindings(logged, value(AUDITED, Map.of()), logged);

        List<AnnotationValue<?>> values = bindings.values();
        assertEquals(2, values.size());
        AnnotationValue<?> read = values.get(0);
        assertEquals(LOGGED, read.getAnnotationName());
        assertEquals("SECONDS", read.stringValue("unit").orElse(null));
        assertEquals("INFO", read.stringValue("level").orElse(null));
        assertFalse(read.contains("comment"));
        assertEquals(List.of("comment"), List.of(read.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)));
        assertEquals(bindings, InterceptorBindings.ofValues(values));
        assertEquals(List.of(), InterceptorBindings.ofValues(List.of()).values());
    }

    @Test
    void theQualifierComparesByThePoint() {
        InterceptorBindings point = bindings(value(LOGGED, Map.of("level", "INFO")));

        assertEquals(InterceptorBindings.boundTo(point),
            InterceptorBindings.boundTo(bindings(AnnotationValue.builder(LOGGED)
                .defaultValues(Map.of("level", "INFO")).build())));
        assertEquals(InterceptorBindings.boundTo(point).hashCode(),
            InterceptorBindings.boundTo(bindings(value(LOGGED, Map.of("level", "INFO")))).hashCode());
        assertNotEquals(InterceptorBindings.boundTo(point),
            InterceptorBindings.boundTo(bindings(value(LOGGED, Map.of("level", "DEBUG")))));
    }
}
