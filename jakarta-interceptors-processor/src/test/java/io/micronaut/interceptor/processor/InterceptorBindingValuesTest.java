package io.micronaut.interceptor.processor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * What a binding is compared by is written out as one string, and the runtime compares nothing but those strings.
 * Two bindings that differ must therefore never be written the same way, whatever their values hold.
 */
class InterceptorBindingValuesTest {

    private static final String BINDING = "test.Tags";

    private static String canonical(Map<CharSequence, Object> values) {
        return InterceptorBindingValues.of(AnnotationValue.builder(BINDING).members(values).build()).canonical();
    }

    @Test
    void anEmptyArrayIsNotAnArrayOfOneEmptyString() {
        assertNotEquals(canonical(Map.of("value", new String[0])), canonical(Map.of("value", new String[]{""})));
    }

    @Test
    void anArrayOfOneEmptyStringIsNotAnArrayOfTwo() {
        assertNotEquals(canonical(Map.of("value", new String[]{""})), canonical(Map.of("value", new String[]{"", ""})));
    }

    @Test
    void anElementHoldingTheSeparatorIsNotTwoElements() {
        assertNotEquals(canonical(Map.of("value", new String[]{"a,b"})),
            canonical(Map.of("value", new String[]{"a", "b"})));
        assertNotEquals(canonical(Map.of("value", new String[]{"a\",\"b"})),
            canonical(Map.of("value", new String[]{"a", "b"})));
    }

    @Test
    void aValueEndingInTheEscapeCharacterDoesNotEscapeWhatFollowsIt() {
        assertNotEquals(canonical(Map.of("first", "a\\", "second", "b")),
            canonical(Map.of("first", "a\\\",second=\"b")));
    }

    @Test
    void aStringIsNotTheArrayItSpellsOut() {
        assertNotEquals(canonical(Map.of("value", "[]")), canonical(Map.of("value", new String[0])));
        assertNotEquals(canonical(Map.of("value", "[\"a\"]")), canonical(Map.of("value", new String[]{"a"})));
    }

    @Test
    void aStringIsNotTheAnnotationItSpellsOut() {
        AnnotationValue<?> nested = AnnotationValue.builder("test.Inner").member("value", "a").build();
        String spelledOut = InterceptorBindingValues.of(nested).canonical();

        assertNotEquals(canonical(Map.of("value", spelledOut)), canonical(Map.of("value", nested)));
    }

    @Test
    void anArrayOfAnnotationsComparesByTheirMembers() {
        AnnotationValue<?> one = AnnotationValue.builder("test.Inner").member("value", "a,b").build();
        AnnotationValue<?> two = AnnotationValue.builder("test.Inner").member("value", "a").build();
        AnnotationValue<?> three = AnnotationValue.builder("test.Inner").member("value", "b").build();

        assertNotEquals(canonical(Map.of("value", new AnnotationValue<?>[]{one})),
            canonical(Map.of("value", new AnnotationValue<?>[]{two, three})));
        assertEquals(canonical(Map.of("value", new AnnotationValue<?>[]{two, three})),
            canonical(Map.of("value", new AnnotationValue<?>[]{
                AnnotationValue.builder("test.Inner").member("value", "a").build(),
                AnnotationValue.builder("test.Inner").member("value", "b").build()})));
    }

    @Test
    void theSameValuesAreWrittenTheSameWay() {
        AnnotationClassValue<?> type = new AnnotationClassValue<>("java.lang.String");
        assertEquals(canonical(Map.of("value", new String[]{"a", ""}, "type", type)),
            canonical(Map.of("type", type, "value", new String[]{"a", ""})));
    }

    /**
     * Micronaut records a class or an enum constant by its name in some places and as itself in others, and a
     * member has one type, so the two are the same value.
     */
    @Test
    void aClassAndAnEnumConstantAreWrittenAsTheirNames() {
        assertEquals(canonical(Map.of("value", new AnnotationClassValue<>("java.lang.String"))),
            canonical(Map.of("value", String.class)));
        assertEquals(canonical(Map.of("value", "SECONDS")),
            canonical(Map.of("value", java.util.concurrent.TimeUnit.SECONDS)));
    }
}
