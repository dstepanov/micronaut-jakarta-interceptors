/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.interceptor.processor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reads the interceptor bindings of an element and reduces each of them to what it is compared by.
 *
 * <p>An interceptor applies to an element when every binding annotation the interceptor declares is also declared
 * by the element, with the same member values. A member excluded from the binding, which is what
 * {@code jakarta.enterprise.util.Nonbinding} does, is left out of the comparison, and the values an annotation
 * defaults to are filled in first, so that {@code @Logged} and {@code @Logged(level = "INFO")} are the same binding
 * when {@code "INFO"} is the default.</p>
 *
 * <p>None of that depends on the running application, so it happens here rather than there: what each binding is
 * compared by is written out on the element, and the runtime compares the strings.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class InterceptorBindingValues {

    private InterceptorBindingValues() {
    }

    /**
     * Makes a member value comparable to the same value read from another element.
     *
     * <p>An array does not compare by its contents, so a binding with an array-valued member would never match
     * another declaration of it. The specification leaves such a member to an extension to define; reading it as
     * the list of its elements is what makes it behave as any other member does.</p>
     *
     * <p>The array types are listed out rather than read through {@code java.lang.reflect.Array}, so that
     * comparing bindings stays as free of the reflection of the platform as the rest of the interception.</p>
     *
     * @param value The value of a member
     * @return The value, or a list of its elements when it is an array
     */
    private static @Nullable Object normalize(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Object[] array -> Arrays.stream(array).map(InterceptorBindingValues::normalize).toList();
            case int[] array -> Arrays.stream(array).boxed().toList();
            case long[] array -> Arrays.stream(array).boxed().toList();
            case double[] array -> Arrays.stream(array).boxed().toList();
            case boolean[] array -> booleans(array);
            case byte[] array -> bytes(array);
            case short[] array -> shorts(array);
            case char[] array -> chars(array);
            case float[] array -> floats(array);
            default -> value;
        };
    }

    private static List<Object> booleans(boolean[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (boolean element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> bytes(byte[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (byte element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> shorts(short[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (short element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> chars(char[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (char element : array) {
            values.add(element);
        }
        return values;
    }

    private static List<Object> floats(float[] array) {
        List<Object> values = new ArrayList<>(array.length);
        for (float element : array) {
            values.add(element);
        }
        return values;
    }

    /**
     * Reads the bindings an element declares.
     *
     * @param annotationMetadata The metadata of the element
     * @return The bindings
     */
    public static Set<Binding> of(AnnotationMetadata annotationMetadata) {
        List<String> names = annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptors.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return Set.of();
        }
        Set<Binding> bindings = new LinkedHashSet<>(names.size());
        for (String name : names) {
            annotationMetadata.findAnnotation(name).map(InterceptorBindingValues::of).ifPresent(bindings::add);
        }
        return bindings;
    }

    /**
     * Reduces one binding annotation to what it binds by.
     *
     * <p>Used at compilation time as well, so that the conflict between two declarations of a binding is decided
     * by exactly what decides whether an interceptor is bound at runtime.</p>
     *
     * @param annotation The binding annotation
     * @return The binding
     */
    public static Binding of(AnnotationValue<?> annotation) {
        // the keys are read as strings so that two bindings compare by the names of their members, which a
        // CharSequence does not promise to do
        Map<String, Object> values = new LinkedHashMap<>();
        Set<String> defaulted = new HashSet<>();
        Map<CharSequence, Object> defaults = annotation.getDefaultValues();
        if (defaults != null) {
            defaults.forEach((member, value) -> {
                values.put(member.toString(), normalize(value));
                defaulted.add(member.toString());
            });
        }
        annotation.getValues().forEach((member, value) -> values.put(member.toString(), normalize(value)));
        // an empty string no default was recorded for is left out, which is what makes a default of "" compare
        // equal whether it is declared or not; see isEmptyString
        values.entrySet().removeIf(entry -> !defaulted.contains(entry.getKey()) && isEmptyString(entry.getValue()));
        for (String nonBinding : annotation.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)) {
            values.remove(nonBinding);
        }
        values.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        return new Binding(annotation.getAnnotationName(), values);
    }

    /**
     * Tells whether a member value is the empty string, which is the one default that Micronaut leaves out of
     * the defaults it records for an annotation compiled from Java or Kotlin.
     *
     * <p>A default is filled in before two bindings are compared, and that only works for a default Micronaut
     * recorded. Leaving out a member that defaults to {@code ""} means {@code @Region("")} carries a
     * {@code value} that {@code @Region} does not, and the two would never bind to each other, although they are
     * the same binding.</p>
     *
     * <p>The member values are what is left to go by, and an empty string declared for a member with no recorded
     * default is left out of the comparison. That is exactly right for each member it can happen to: one whose
     * default is {@code ""} reads the same whether the value is declared or not, and one without any default is
     * declared by every declaration of the binding, so leaving the empty value out on both sides of the comparison
     * changes nothing about whether they are equal. A member whose default Micronaut did record keeps an empty
     * string, which differs from that default.</p>
     *
     * <p>Reading the defaults off the annotation type instead does not work for every language: what the visitor
     * context offers for it answers all of them for Java, none for Kotlin, and for Groovy only the ones that are
     * a single constant.</p>
     *
     * @param value The normalized value of a member
     * @return Whether the value is an empty string
     */
    private static boolean isEmptyString(@Nullable Object value) {
        return value instanceof String string && string.isEmpty();
    }

    /**
     * A binding annotation reduced to what it binds by: its name and the member values that take part in the
     * binding.
     *
     * @param name   The annotation name
     * @param values The member values
     */
    public record Binding(String name, Map<String, Object> values) {

        /**
         * Writes the binding out as one string, so that two of them can be compared without reading either
         * annotation again.
         *
         * <p>The members are written in the order of their names rather than the order they were declared in, so
         * that an interceptor and the element it intercepts produce the same string for the same binding however
         * either of them wrote it.</p>
         *
         * <p>No two bindings that differ are written the same way. Every value that is not an array or an
         * annotation is written between double quotes, with a backslash before a double quote or a backslash it
         * holds, so a value is read back as exactly the characters between its quotes whatever punctuation it
         * carries. Without the quotes, a value holding a comma or an equals sign would read as two members -
         * {@code @Sized(a = "1,b=2")} as {@code @Sized(a = "1", b = "2")} - and an empty string would be written as
         * nothing at all, which made {@code @Tags({""})}, an array of one empty string, read as {@code @Tags({})},
         * an empty one. Names are written as they are: the name of an annotation or of a member is made of
         * identifiers, which hold no quote, comma, parenthesis or bracket, so whether a value is a string, an
         * array, an annotation or {@code null} is told by the character it starts with.</p>
         *
         * <p>A class and its name, and an enum constant and its name, are written alike, as are a character and
         * the string of that one character. A member has one type, so none of those pairs can tell two bindings
         * of the same member apart, and Micronaut records a class or an enum constant by its name in some places
         * and as itself in others.</p>
         *
         * @return The binding as a string
         */
        public String canonical() {
            StringBuilder builder = new StringBuilder();
            write(builder);
            return builder.toString();
        }

        private void write(StringBuilder builder) {
            builder.append(name).append('(');
            boolean first = true;
            for (Map.Entry<String, Object> entry : new TreeMap<>(values).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(entry.getKey()).append('=');
                render(builder, entry.getValue());
            }
            builder.append(')');
        }

        private static void render(StringBuilder builder, @Nullable Object value) {
            switch (value) {
                case null -> builder.append("null");
                case AnnotationClassValue<?> classValue -> quote(builder, classValue.getName());
                case Class<?> type -> quote(builder, type.getName());
                case AnnotationValue<?> annotation -> of(annotation).write(builder);
                case Enum<?> constant -> quote(builder, constant.name());
                case List<?> list -> {
                    builder.append('[');
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) {
                            builder.append(',');
                        }
                        render(builder, list.get(i));
                    }
                    builder.append(']');
                }
                default -> quote(builder, value.toString());
            }
        }

        private static void quote(StringBuilder builder, String text) {
            builder.append('"');
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (character == '"' || character == '\\') {
                    builder.append('\\');
                }
                builder.append(character);
            }
            builder.append('"');
        }
    }
}
