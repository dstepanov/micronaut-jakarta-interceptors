package io.micronaut.interceptor.processor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.spi.InterceptorBindings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a binding is compared by at compilation time, and the value it is written out as for the runtime to compare.
 * Two bindings that differ must never be the same binding, whatever their values hold, and the value written out
 * must be the same binding at runtime as the one it was read from.
 */
class InterceptorBindingValuesTest {

    private static final String BINDING = "test.Tags";

    private static InterceptorBindingValues.Binding binding(Map<CharSequence, Object> values) {
        return InterceptorBindingValues.of(AnnotationValue.builder(BINDING).members(values).build());
    }

    private static InterceptorBindings written(InterceptorBindingValues.Binding binding) {
        return InterceptorBindings.ofValues(List.of(binding.written()));
    }

    @Test
    void anEmptyArrayIsNotAnArrayOfOneEmptyString() {
        assertNotEquals(binding(Map.of("value", new String[0])), binding(Map.of("value", new String[]{""})));
        assertNotEquals(written(binding(Map.of("value", new String[0]))),
            written(binding(Map.of("value", new String[]{""}))));
    }

    @Test
    void anArrayOfOneEmptyStringIsNotAnArrayOfTwo() {
        assertNotEquals(binding(Map.of("value", new String[]{""})), binding(Map.of("value", new String[]{"", ""})));
    }

    @Test
    void anElementHoldingASeparatorIsNotTwoElements() {
        assertNotEquals(binding(Map.of("value", new String[]{"a,b"})), binding(Map.of("value", new String[]{"a", "b"})));
        assertNotEquals(written(binding(Map.of("value", new String[]{"a,b"}))),
            written(binding(Map.of("value", new String[]{"a", "b"}))));
    }

    @Test
    void aStringIsNotTheArrayItSpellsOut() {
        assertNotEquals(binding(Map.of("value", "[]")), binding(Map.of("value", new String[0])));
        assertNotEquals(binding(Map.of("value", "[\"a\"]")), binding(Map.of("value", new String[]{"a"})));
    }

    @Test
    void aStringIsNotTheAnnotationItSpellsOut() {
        AnnotationValue<?> nested = AnnotationValue.builder("test.Inner").member("value", "a").build();

        assertNotEquals(binding(Map.of("value", nested.toString())), binding(Map.of("value", nested)));
    }

    @Test
    void anArrayOfAnnotationsComparesByTheirMembers() {
        AnnotationValue<?> one = AnnotationValue.builder("test.Inner").member("value", "a,b").build();
        AnnotationValue<?> two = AnnotationValue.builder("test.Inner").member("value", "a").build();
        AnnotationValue<?> three = AnnotationValue.builder("test.Inner").member("value", "b").build();
        AnnotationValue<?>[] again = {
            AnnotationValue.builder("test.Inner").member("value", "a").build(),
            AnnotationValue.builder("test.Inner").member("value", "b").build()};

        assertNotEquals(binding(Map.of("value", new AnnotationValue<?>[]{one})),
            binding(Map.of("value", new AnnotationValue<?>[]{two, three})));
        assertEquals(binding(Map.of("value", new AnnotationValue<?>[]{two, three})), binding(Map.of("value", again)));
        assertEquals(written(binding(Map.of("value", new AnnotationValue<?>[]{two, three}))),
            written(binding(Map.of("value", again))));
        assertNotEquals(written(binding(Map.of("value", new AnnotationValue<?>[]{one}))),
            written(binding(Map.of("value", new AnnotationValue<?>[]{two, three}))));
    }

    @Test
    void theOrderOfTheMembersSaysNothing() {
        AnnotationClassValue<?> type = new AnnotationClassValue<>("java.lang.String");
        assertEquals(binding(Map.of("value", new String[]{"a", ""}, "type", type)),
            binding(Map.of("type", type, "value", new String[]{"a", ""})));
    }

    /**
     * Micronaut records a class or an enum constant by its name in some places and as itself in others, and a
     * member has one type, so the two are the same value. The binding is written out the way Micronaut records it.
     */
    @Test
    void aClassAndAnEnumConstantAreTheirNames() {
        InterceptorBindingValues.Binding byClass = binding(Map.of("value", String.class));
        InterceptorBindingValues.Binding byEnum = binding(Map.of("value", TimeUnit.SECONDS));

        assertEquals(binding(Map.of("value", new AnnotationClassValue<>("java.lang.String"))), byClass);
        assertEquals(binding(Map.of("value", "SECONDS")), byEnum);
        assertEquals(new AnnotationClassValue<>("java.lang.String"), byClass.written().getValues().get("value"));
        assertEquals("SECONDS", byEnum.written().getValues().get("value"));
        assertArrayEquals(new String[]{"SECONDS", "MINUTES"}, (Object[]) binding(
            Map.of("value", new TimeUnit[]{TimeUnit.SECONDS, TimeUnit.MINUTES})).written().getValues().get("value"));
    }

    /**
     * A member excluded from the binding is left out of the value written out, and listed under
     * {@code $nonBinding}, whether the occurrence recorded it or only the annotation type excludes it.
     */
    @Test
    void theExcludedMembersAreLeftOutAndListed() {
        AnnotationValue<?> declared = AnnotationValue.builder(BINDING)
            .member("value", "a")
            .member("comment", "x")
            .member("note", "y")
            .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"comment"})
            .build();

        InterceptorBindingValues.Binding binding =
            InterceptorBindingValues.of(declared, name -> List.of("note"));

        assertEquals(Map.of("value", "a"), binding.values());
        AnnotationValue<?> written = binding.written();
        assertFalse(written.contains("comment"));
        assertFalse(written.contains("note"));
        assertArrayEquals(new String[]{"comment", "note"}, written.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE));
        assertTrue(InterceptorBindings.ofValues(List.of(written)).isBoundBy(InterceptorBindings.ofValues(List.of(
            AnnotationValue.builder(BINDING).member("value", "a").member("comment", "z").member("note", "w").build()))));
    }

    /**
     * An empty string with no default recorded for it reads as the member left out, which is what a Java annotation
     * defaulting to {@code ""} records.
     */
    @Test
    void anEmptyStringWithoutADefaultIsTheMemberLeftOut() {
        assertEquals(binding(Map.of()), binding(Map.of("value", "")));
        assertFalse(binding(Map.of("value", "")).written().contains("value"));
    }
}
