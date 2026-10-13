package io.micronaut.interceptor.processor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.interceptor.internal.metadata.BindingValue;
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

    private static BindingValue binding(Map<CharSequence, Object> values) {
        return InterceptorBindingValues.of(AnnotationValue.builder(BINDING).members(values).build(), name -> List.of());
    }

    private static InterceptorBindings written(BindingValue binding) {
        return InterceptorBindings.ofRecorded(List.of(binding.toAnnotationValue()));
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
        BindingValue byClass = binding(Map.of("value", String.class));
        BindingValue byEnum = binding(Map.of("value", TimeUnit.SECONDS));

        assertEquals(binding(Map.of("value", new AnnotationClassValue<>("java.lang.String"))), byClass);
        assertEquals(binding(Map.of("value", "SECONDS")), byEnum);
        assertEquals(new AnnotationClassValue<>("java.lang.String"), byClass.toAnnotationValue().getValues().get("value"));
        assertEquals("SECONDS", byEnum.toAnnotationValue().getValues().get("value"));
        assertArrayEquals(new String[]{"SECONDS", "MINUTES"}, (Object[]) binding(
            Map.of("value", new TimeUnit[]{TimeUnit.SECONDS, TimeUnit.MINUTES})).toAnnotationValue().getValues().get("value"));
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

        BindingValue binding =
            InterceptorBindingValues.of(declared, name -> List.of("note"));

        assertEquals(Map.of("value", "a"), binding.members());
        AnnotationValue<?> written = binding.toAnnotationValue();
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
        assertFalse(binding(Map.of("value", "")).toAnnotationValue().contains("value"));
    }

    /**
     * An empty string where the annotation defaults to another value is written out, and is not that default: it is
     * compared as the member left out, against a default that is there.
     */
    @Test
    void anEmptyStringWhereTheDefaultIsAnotherValueIsNotTheDefault() {
        AnnotationValue<?> empty = new AnnotationValue<>(BINDING, Map.of("value", ""), Map.of("value", "eu"));
        AnnotationValue<?> defaulted = new AnnotationValue<>(BINDING, Map.of(), Map.of("value", "eu"));

        BindingValue reduced = InterceptorBindingValues.of(empty, name -> List.of());
        assertEquals("", reduced.toAnnotationValue().getValues().get("value"));
        assertNotEquals(reduced, InterceptorBindingValues.of(defaulted, name -> List.of()));
        assertFalse(written(reduced).isBoundBy(InterceptorBindings.ofValues(List.of(defaulted))));
        assertTrue(written(reduced).isBoundBy(InterceptorBindings.ofValues(List.of(empty))));
    }

    /**
     * What a binding is written out as is the caller's: changing an array it holds changes nothing of the binding.
     */
    @Test
    void theValueWrittenOutSharesNoArrayWithTheBinding() {
        BindingValue binding = binding(Map.of("value", new String[]{"a"}, "sizes", new int[]{1}));

        ((String[]) binding.toAnnotationValue().getValues().get("value"))[0] = "b";
        ((int[]) binding.toAnnotationValue().getValues().get("sizes"))[0] = 2;

        assertArrayEquals(new String[]{"a"}, (String[]) binding.toAnnotationValue().getValues().get("value"));
        assertArrayEquals(new int[]{1}, (int[]) binding.toAnnotationValue().getValues().get("sizes"));
    }
}
