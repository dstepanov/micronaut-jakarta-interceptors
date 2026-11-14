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
package io.micronaut.interceptor.internal.metadata;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.core.util.CollectionUtils;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * One interceptor binding, reduced to what it binds by, as the processor records it. The runtime compares what was
 * recorded with {@link AnnotationValue#matches(AnnotationValue)} as it was read, and so does this class, so that
 * the two decide alike whether two bindings are one.
 *
 * <p>A binding is reduced to the members that take part in it:</p>
 * <ul>
 *     <li>the members the annotation defaults to are filled in, so that {@code @Logged} and
 *     {@code @Logged(level = "INFO")} are the same binding where {@code "INFO"} is the default, and what is recorded
 *     reads the same whatever defaults Micronaut registers for the annotation later;</li>
 *     <li>the members excluded from the binding, which is what {@code jakarta.enterprise.util.Nonbinding} does, are
 *     left out and listed under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE};</li>
 *     <li>an empty string the processor finds no default for is left out of what it records, as Micronaut records
 *     no default of an empty string for an annotation compiled from Java or Kotlin, so that {@code @Region("")} and
 *     {@code @Region} are recorded alike;</li>
 *     <li>an enum constant is spelt by its name and a class as an {@link AnnotationClassValue}, as Micronaut records
 *     them; an annotation held by a member is reduced as a binding is; and an evaluated expression is the
 *     expression it was written as.</li>
 * </ul>
 *
 * <p>Two bindings so reduced are compared, and hashed, by Micronaut: {@link AnnotationValue#matches(AnnotationValue)}
 * and {@link AnnotationValue#bindingHashCode()}, of the binding as a value of its annotation with the members it
 * excludes listed and no defaults of its own, since every member that binds is there. Micronaut compares an empty
 * string with a member left out with no default, an enum constant with its name, a class with its class value, a
 * primitive array with its boxed elements, and an annotation a member holds as a binding, leaving out the members
 * either side excludes.</p>
 *
 * <p>{@link AnnotationValue#equals(Object)} compares none of this. It passes over a member the other value does
 * not have, so two values that declare different members of the same count compare equal.</p>
 *
 * <p>The array types are listed out rather than read through {@code java.lang.reflect.Array}, so that copying
 * bindings stays as free of the reflection of the platform as the rest of the interception.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class BindingValue {

    private final String name;
    private final Set<String> nonBinding;
    private final Map<String, Object> members;
    // the members excluded from the binding, as the declaration gives them: recorded so that the bindings an
    // invocation reports are the declaration, and left out of every comparison by the list of excluded members
    private final Map<String, Object> excludedMembers;
    // the binding as Micronaut compares and hashes two annotations, see matches
    private final AnnotationValue<?> matched;

    private BindingValue(String name, Set<String> nonBinding, Map<String, Object> members,
                         Map<String, Object> excludedMembers) {
        this.name = name;
        this.nonBinding = nonBinding;
        this.members = members;
        this.excludedMembers = excludedMembers;
        Map<CharSequence, Object> matchedMembers = CollectionUtils.newLinkedHashMap(members.size() + 1);
        matchedMembers.putAll(members);
        if (!nonBinding.isEmpty()) {
            matchedMembers.put(AnnotationUtil.NON_BINDING_ATTRIBUTE, nonBinding.toArray(String[]::new));
        }
        // the value toAnnotationValue writes out, sharing the arrays of the binding, and with no defaults of its own:
        // every member that binds is there
        this.matched = new AnnotationValue<>(name, Collections.unmodifiableMap(matchedMembers), Collections.emptyMap());
    }

    /**
     * Reduces a binding annotation as the processor records it.
     *
     * <p>Micronaut records every member excluded from a binding on each occurrence of the annotation, whether or not
     * the occurrence supplied a value for it, and on an annotation a member of another holds as well, and those are
     * the ones read.</p>
     *
     * <p>An empty string the value carries no default for is left out of what is recorded, which reads the same
     * whether it was declared or not. An empty string where a default was recorded is recorded, and differs from the
     * default the other side has filled in.</p>
     *
     * @param annotation The binding annotation, as declared on an element
     * @return The binding
     */
    public static BindingValue reduce(AnnotationValue<?> annotation) {
        String name = annotation.getAnnotationName();
        // the keys are read as strings so that two bindings compare by the names of their members, which a
        // CharSequence does not promise to do
        Map<String, Object> members = new LinkedHashMap<>();
        Set<String> defaulted = new TreeSet<>();
        Map<CharSequence, Object> defaults = annotation.getDefaultValues();
        if (defaults != null) {
            defaults.forEach((member, value) -> {
                members.put(member.toString(), value);
                defaulted.add(member.toString());
            });
        }
        annotation.getValues().forEach((member, value) -> members.put(member.toString(), value));
        members.entrySet().removeIf(entry -> !defaulted.contains(entry.getKey()) && isEmptyString(entry.getValue()));
        Set<String> nonBinding = new TreeSet<>(Arrays.asList(annotation.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)));
        // Micronaut lists the member it records them under among them, which is no member of the annotation; kept,
        // it would set a binding read from the metadata apart from the same binding as recorded
        nonBinding.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        members.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        members.remove(AnnotationUtil.STEREOTYPES_MEMBER);
        Map<String, Object> excludedMembers = new LinkedHashMap<>();
        annotation.getValues().forEach((member, value) -> {
            if (nonBinding.contains(member.toString())) {
                excludedMembers.put(member.toString(), writable(value));
            }
        });
        members.keySet().removeAll(nonBinding);
        Map<String, Object> written = CollectionUtils.newLinkedHashMap(members.size());
        members.forEach((member, value) -> written.put(member, writable(value)));
        return new BindingValue(name, Collections.unmodifiableSet(nonBinding), Collections.unmodifiableMap(written),
            Collections.unmodifiableMap(excludedMembers));
    }

    /**
     * @return The name of the binding annotation
     */
    public String name() {
        return name;
    }

    /**
     * The binding as a value of its annotation, as the processor records it: the members that bind, spelt as
     * Micronaut records them, the members excluded from it as the declaration gives them, and the excluded members
     * listed under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}, which leaves them out of every comparison.
     *
     * <p>Every call builds a value of its own, whose arrays are copies: what is done to them changes nothing of the
     * binding.</p>
     *
     * @return The value
     */
    public AnnotationValue<?> toAnnotationValue() {
        Map<CharSequence, Object> written = CollectionUtils.newLinkedHashMap(members.size() + excludedMembers.size() + 1);
        members.forEach((member, value) -> written.put(member, copyOf(value)));
        excludedMembers.forEach((member, value) -> written.put(member, copyOf(value)));
        // the excluded members are listed for whatever the binding is compared with, which may be the value of an
        // annotation instance that carries all of its members and says nothing of which bind
        if (!nonBinding.isEmpty()) {
            written.put(AnnotationUtil.NON_BINDING_ATTRIBUTE, nonBinding.toArray(String[]::new));
        }
        return AnnotationValue.builder(name).members(written).build();
    }

    /**
     * Whether this and another binding are the same binding, compared by Micronaut,
     * {@link AnnotationValue#matches(AnnotationValue)}, without the members either of them lists as excluded.
     *
     * @param other The other binding
     * @return Whether the two are the same binding
     */
    public boolean matches(BindingValue other) {
        return matched.matches(other.matched);
    }

    /**
     * Whether this and another binding are the same declaration: the values they are written out as are equal member
     * for member, the members excluded from the binding included, and so are those of an annotation a member holds,
     * at any depth. An element declaring a binding that differs from the one of its class only in a member excluded
     * from it is bound alike, but reports its own declaration.
     *
     * @param other The other binding
     * @return Whether the two are the same declaration
     */
    public boolean isSameDeclaration(BindingValue other) {
        return sameValue(toAnnotationValue(), other.toAnnotationValue());
    }

    private static boolean sameValue(@Nullable Object value, @Nullable Object other) {
        if (value instanceof AnnotationValue<?> annotation) {
            return other instanceof AnnotationValue<?> otherAnnotation
                && annotation.getAnnotationName().equals(otherAnnotation.getAnnotationName())
                && sameMembers(annotation.getValues(), otherAnnotation.getValues());
        }
        if (value instanceof Object[] values) {
            if (!(other instanceof Object[] others) || values.length != others.length) {
                return false;
            }
            for (int i = 0; i < values.length; i++) {
                if (!sameValue(values[i], others[i])) {
                    return false;
                }
            }
            return true;
        }
        return Objects.deepEquals(value, other);
    }

    private static boolean sameMembers(Map<CharSequence, Object> members, Map<CharSequence, Object> others) {
        if (members.size() != others.size()) {
            return false;
        }
        for (Map.Entry<CharSequence, Object> member : members.entrySet()) {
            if (!others.containsKey(member.getKey()) || !sameValue(member.getValue(), others.get(member.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Two bindings are equal when they {@link #matches(BindingValue) match}, which is what the processor tells two
     * declarations of a binding apart by. The members excluded take no part.
     *
     * @param o The other object
     * @return Whether the two are equal
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || (o instanceof BindingValue other && matches(other));
    }

    /**
     * Micronaut's hash code of the binding, {@link AnnotationValue#bindingHashCode()}, which agrees with
     * {@link #matches(BindingValue)}: the binding declares every member that binds and lists the ones it excludes.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return matched.bindingHashCode();
    }

    @Override
    public String toString() {
        return matched.toString();
    }

    /**
     * A copy of a member value as it is written out, so that the value handed out shares no array with the binding,
     * an annotation it holds included.
     */
    private static Object copyOf(Object value) {
        if (value instanceof AnnotationValue<?> annotation) {
            return copyOf(annotation);
        }
        if (value instanceof AnnotationValue<?>[] annotations) {
            AnnotationValue<?>[] copies = new AnnotationValue<?>[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                copies[i] = copyOf(annotations[i]);
            }
            return copies;
        }
        return switch (value) {
            case Object[] array -> array.clone();
            case int[] array -> array.clone();
            case long[] array -> array.clone();
            case double[] array -> array.clone();
            case boolean[] array -> array.clone();
            case byte[] array -> array.clone();
            case short[] array -> array.clone();
            case char[] array -> array.clone();
            case float[] array -> array.clone();
            default -> value;
        };
    }

    private static AnnotationValue<?> copyOf(AnnotationValue<?> annotation) {
        Map<CharSequence, Object> values = annotation.getValues();
        Map<CharSequence, Object> copies = CollectionUtils.newLinkedHashMap(values.size());
        values.forEach((member, value) -> copies.put(member, copyOf(value)));
        return AnnotationValue.builder(annotation.getAnnotationName()).members(copies).build();
    }

    /**
     * Tells whether a member value is the empty string, which the processor leaves out where no default is recorded.
     */
    private static boolean isEmptyString(@Nullable Object value) {
        return value instanceof String string && string.isEmpty();
    }

    /**
     * Spells a member value the way a binding is recorded: as Micronaut records it, an enum constant by its name and
     * a class as an {@link AnnotationClassValue}, with an annotation it holds reduced as a binding is. An array keeps
     * its type, which is what Micronaut writes the metadata of an annotation from.
     */
    private static Object writable(Object value) {
        if (value instanceof AnnotationValue<?> annotation) {
            return reduce(annotation).toAnnotationValue();
        }
        if (value instanceof AnnotationValue<?>[] annotations) {
            AnnotationValue<?>[] written = new AnnotationValue<?>[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                written[i] = reduce(annotations[i]).toAnnotationValue();
            }
            return written;
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        if (value instanceof Enum<?>[] constants) {
            return Arrays.stream(constants).map(Enum::name).toArray(String[]::new);
        }
        if (value instanceof Class<?> type) {
            return new AnnotationClassValue<>(type.getName());
        }
        if (value instanceof Class<?>[] types) {
            return Arrays.stream(types)
                .map(type -> new AnnotationClassValue<>(type.getName()))
                .toArray(AnnotationClassValue<?>[]::new);
        }
        if (value instanceof EvaluatedExpressionReference expression) {
            return writable(expression.annotationValue());
        }
        // an array is copied, as the one the binding was read from stays the caller's
        return copyOf(value);
    }
}
