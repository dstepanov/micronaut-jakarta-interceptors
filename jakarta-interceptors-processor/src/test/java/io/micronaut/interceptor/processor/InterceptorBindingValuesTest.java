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
        return InterceptorBindingValues.of(AnnotationValue.builder(BINDING).members(values).build());
    }

    private static InterceptorBindings written(BindingValue binding) {
        return InterceptorBindings.ofValues(List.of(binding.toAnnotationValue()));
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
     * Two bindings that are equal hash alike, however their values spell the same members: the processor keeps them
     * in sets and maps.
     */
    @Test
    void equalBindingsHashAlike() {
        List<List<BindingValue>> alike = List.of(
            List.of(binding(Map.of("value", String.class)),
                binding(Map.of("value", new AnnotationClassValue<>("java.lang.String")))),
            List.of(binding(Map.of("value", TimeUnit.SECONDS)), binding(Map.of("value", "SECONDS"))),
            List.of(binding(Map.of("value", new TimeUnit[]{TimeUnit.SECONDS})),
                binding(Map.of("value", new String[]{"SECONDS"}))),
            List.of(binding(Map.of("value", new int[]{1, 2})), InterceptorBindingValues.of(
                new AnnotationValue<>(BINDING, Map.of("value", new Integer[]{1, 2}), Map.of()))),
            List.of(binding(Map.of()), binding(Map.of("value", ""))),
            List.of(binding(Map.of("value", AnnotationValue.builder("test.Inner").member("value", "a").build())),
                binding(Map.of("value", AnnotationValue.builder("test.Inner").member("value", "a").build()))));
        for (List<BindingValue> pair : alike) {
            assertEquals(pair.get(0), pair.get(1));
            assertEquals(pair.get(0).hashCode(), pair.get(1).hashCode(), pair.toString());
        }
        assertNotEquals(binding(Map.of("value", new int[]{1, 2})), binding(Map.of("value", new int[]{2, 1})));
    }

    /**
     * A member excluded from the binding is left out of the value written out, and listed under
     * {@code $nonBinding}. The ones an occurrence records are read off it, which Micronaut lists along with the
     * member it lists them under; the type's are not asked for, as Micronaut records every one of them.
     */
    @Test
    void theExcludedMembersAreKeptAsDeclaredAndListed() {
        AnnotationValue<?> declared = AnnotationValue.builder(BINDING)
            .member("value", "a")
            .member("comment", "x")
            .member("note", "y")
            .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"comment", "note",
                AnnotationUtil.NON_BINDING_ATTRIBUTE})
            .build();

        BindingValue binding =
            InterceptorBindingValues.of(declared);

        AnnotationValue<?> written = binding.toAnnotationValue();
        assertEquals("a", written.stringValue().orElse(null));
        assertEquals("x", written.stringValue("comment").orElse(null));
        assertEquals("y", written.stringValue("note").orElse(null));
        assertArrayEquals(new String[]{"comment", "note"}, written.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE));
        assertEquals(binding, InterceptorBindingValues.of(AnnotationValue.builder(BINDING)
            .member("value", "a")
            .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"comment", "note"})
            .build()));
        assertTrue(InterceptorBindings.ofValues(List.of(written)).isBoundBy(InterceptorBindings.ofValues(List.of(
            AnnotationValue.builder(BINDING).member("value", "a").member("comment", "z").member("note", "w").build()))));
    }

    /**
     * An annotation a member of a binding holds lists the members its type excludes, as Micronaut records them on it,
     * and they are kept as declared and listed, as those of the binding are.
     */
    @Test
    void anAnnotationAMemberHoldsListsTheMembersItsTypeExcludes() {
        AnnotationValue<?> held = AnnotationValue.builder("test.Held")
            .member("value", "a")
            .member("note", "y")
            .member(AnnotationUtil.NON_BINDING_ATTRIBUTE, new String[]{"note"})
            .build();
        AnnotationValue<?> declared = AnnotationValue.builder(BINDING).member("value", held).build();

        BindingValue binding = InterceptorBindingValues.of(declared);

        AnnotationValue<?> written = binding.toAnnotationValue().getAnnotation("value").orElseThrow();
        assertEquals("a", written.stringValue().orElse(null));
        assertEquals("y", written.stringValue("note").orElse(null));
        assertArrayEquals(new String[]{"note"}, written.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE));
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

        BindingValue reduced = InterceptorBindingValues.of(empty);
        assertEquals("", reduced.toAnnotationValue().getValues().get("value"));
        assertNotEquals(reduced, InterceptorBindingValues.of(defaulted));
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
