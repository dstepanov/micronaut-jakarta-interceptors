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
        Map<CharSequence, Object> defaults = annotation.getDefaultValues();
        if (defaults != null) {
            defaults.forEach((member, value) -> values.put(member.toString(), normalize(value)));
        }
        annotation.getValues().forEach((member, value) -> values.put(member.toString(), normalize(value)));
        for (String nonBinding : annotation.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)) {
            values.remove(nonBinding);
        }
        values.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        return new Binding(annotation.getAnnotationName(), values);
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
         * The characters {@link #canonical()} punctuates a binding with, which are written with a backslash
         * before them wherever they occur in a value. The backslash itself is escaped too, so that a value
         * ending in one cannot escape the punctuation that follows it.
         */
        private static final String PUNCTUATION = "\\,=()[]";

        /**
         * Writes the binding out as one string, so that two of them can be compared without reading either
         * annotation again.
         *
         * <p>The members are written in the order of their names rather than the order they were declared in, so
         * that an interceptor and the element it intercepts produce the same string for the same binding however
         * either of them wrote it.</p>
         *
         * <p>The characters the string is punctuated with are escaped wherever they occur in a value, so that no
         * two bindings that differ can be written the same way. Without that, a member whose value contains a
         * comma or an equals sign would read as two members, and {@code @Sized(a = "1,b=2")} would be
         * indistinguishable from {@code @Sized(a = "1", b = "2")}.</p>
         *
         * @return The binding as a string
         */
        public String canonical() {
            StringBuilder builder = new StringBuilder(name).append('(');
            boolean first = true;
            for (Map.Entry<String, Object> entry : new TreeMap<>(values).entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(entry.getKey()).append('=');
                render(builder, entry.getValue());
            }
            return builder.append(')').toString();
        }

        private static void render(StringBuilder builder, @Nullable Object value) {
            switch (value) {
                case null -> builder.append("null");
                case AnnotationClassValue<?> classValue -> builder.append(classValue.getName());
                case Class<?> type -> builder.append(type.getName());
                case AnnotationValue<?> annotation -> builder.append(of(annotation).canonical());
                case Enum<?> constant -> builder.append(constant.name());
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
                // a member of any other type is written as it reads, which for a string or a character is
                // whatever the source wrote. That is the one value that can carry the punctuation of the
                // string it is being written into, so it is the one that is escaped. A name - of an
                // annotation, of a member, of a class or of an enum constant - cannot
                default -> escape(builder, value.toString());
            }
        }

        private static void escape(StringBuilder builder, String text) {
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (PUNCTUATION.indexOf(character) >= 0) {
                    builder.append('\\');
                }
                builder.append(character);
            }
        }
    }
}
