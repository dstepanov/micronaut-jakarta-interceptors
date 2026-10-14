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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * One interceptor binding, reduced to what it binds by. The processor reduces a binding with it before it records
 * it, and the runtime reads what was recorded with it, so that the two decide alike whether two bindings are one.
 *
 * <p>A binding is reduced to the members that take part in it:</p>
 * <ul>
 *     <li>the members the annotation defaults to are filled in, so that {@code @Logged} and
 *     {@code @Logged(level = "INFO")} are the same binding where {@code "INFO"} is the default. The defaults are
 *     those the value carries. A value the processor recorded has every default filled in already, and is read
 *     without them: Micronaut keeps the defaults of an annotation in a registry the whole application shares, which
 *     the reflection module adds to the first time it builds an instance of the annotation, so reading the
 *     defaults again would make the same recorded value read differently before and after;</li>
 *     <li>the members excluded from the binding, which is what {@code jakarta.enterprise.util.Nonbinding} does, are
 *     left out and listed under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}, and two bindings are compared without
 *     the members either of them lists;</li>
 *     <li>a member that is an empty string is compared as if it were left out, once the defaults are filled in.
 *     Micronaut records no default of an empty string for an annotation compiled from Java or Kotlin, so
 *     {@code @Region("")} carries a member that {@code @Region} does not, although the two are the same binding; and
 *     a member without a default is declared by every declaration of the binding, so leaving out an empty string on
 *     both sides changes nothing about whether two of them are equal. Where the annotation defaults the member to
 *     another value, an empty string still differs from it: the declaration that leaves the member out has the
 *     default filled in, and the one that declares the empty string has nothing there;</li>
 *     <li>an enum constant is its name and a class an {@link AnnotationClassValue}, which is how Micronaut records
 *     them in some places and not in others; an array is the list of its elements, compared by its contents; an
 *     annotation held by a member is reduced as a binding is; and an evaluated expression is the expression it
 *     was written as.</li>
 * </ul>
 *
 * <p>{@link AnnotationValue#equals(Object)} compares none of this. It passes over a member the other value does
 * not have, so two values that declare different members of the same count compare equal.</p>
 *
 * <p>The array types are listed out rather than read through {@code java.lang.reflect.Array}, so that comparing
 * bindings stays as free of the reflection of the platform as the rest of the interception.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class BindingValue {

    private static final Function<String, Collection<String>> NONE_EXCLUDED = name -> List.of();

    private final String name;
    private final Set<String> nonBinding;
    private final Map<String, Object> members;
    private final Map<String, Object> compared;

    private BindingValue(String name, Set<String> nonBinding, Map<String, Object> members) {
        this.name = name;
        this.nonBinding = nonBinding;
        this.members = members;
        Map<String, Object> normalized = new LinkedHashMap<>(members.size());
        members.forEach((member, value) -> {
            if (!isEmptyString(value)) {
                normalized.put(member, normalize(value));
            }
        });
        this.compared = Collections.unmodifiableMap(normalized);
    }

    /**
     * Reduces a binding annotation as the processor records it.
     *
     * <p>Micronaut records every member excluded from a binding on each occurrence of the annotation, whether or not
     * the occurrence supplied a value for it, and those are the ones read. An annotation held by a member of another
     * is recorded without them, so the members excluded from the type of such an annotation are supplied, which the
     * processor reads off the type.</p>
     *
     * <p>An empty string the value carries no default for is left out of what is recorded, which reads the same
     * whether it was declared or not. An empty string where a default was recorded is recorded: compared, it is the
     * member left out, as every empty string is, and that differs from the default the other side has filled in.</p>
     *
     * @param annotation The binding annotation, as declared on an element
     * @param excluded   The members excluded from an annotation a member holds, by the name of the annotation
     * @return The binding
     */
    public static BindingValue reduce(AnnotationValue<?> annotation, Function<String, ? extends Collection<String>> excluded) {
        return of(annotation, excluded, false, true, true);
    }

    /**
     * Reads a binding the processor recorded, which has its defaults filled in already and is read without them.
     *
     * @param recorded The binding as recorded
     * @return The binding
     */
    public static BindingValue ofRecorded(AnnotationValue<?> recorded) {
        return of(recorded, NONE_EXCLUDED, false, false, false);
    }

    /**
     * Reads a binding from any value of its annotation - one read from the metadata of an element, or one built from
     * an annotation instance - with the defaults the value carries filled in.
     *
     * @param value The value of the binding annotation
     * @return The binding
     */
    public static BindingValue of(AnnotationValue<?> value) {
        return of(value, NONE_EXCLUDED, false, true, false);
    }

    private static BindingValue of(AnnotationValue<?> annotation,
                                   Function<String, ? extends Collection<String>> excluded,
                                   boolean nested,
                                   boolean withDefaults,
                                   boolean leaveOutUndefaultedEmptyStrings) {
        String name = annotation.getAnnotationName();
        // the keys are read as strings so that two bindings compare by the names of their members, which a
        // CharSequence does not promise to do
        Map<String, Object> members = new LinkedHashMap<>();
        Set<String> defaulted = new TreeSet<>();
        Map<CharSequence, Object> defaults = withDefaults ? annotation.getDefaultValues() : null;
        if (defaults != null) {
            defaults.forEach((member, value) -> {
                members.put(member.toString(), value);
                defaulted.add(member.toString());
            });
        }
        annotation.getValues().forEach((member, value) -> members.put(member.toString(), value));
        if (leaveOutUndefaultedEmptyStrings) {
            members.entrySet().removeIf(entry -> !defaulted.contains(entry.getKey()) && isEmptyString(entry.getValue()));
        }
        Set<String> nonBinding = new TreeSet<>(Arrays.asList(annotation.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)));
        // Micronaut lists the member it records them under among them, which is no member of the annotation; kept,
        // it would set a binding read from the metadata apart from the same binding as recorded
        nonBinding.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        if (nested) {
            nonBinding.addAll(excluded.apply(name));
        }
        members.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
        members.remove(AnnotationUtil.STEREOTYPES_MEMBER);
        members.keySet().removeAll(nonBinding);
        Map<String, Object> written = new LinkedHashMap<>(members.size());
        members.forEach((member, value) -> written.put(member, writable(value, excluded, withDefaults,
            leaveOutUndefaultedEmptyStrings)));
        return new BindingValue(name, Collections.unmodifiableSet(nonBinding), Collections.unmodifiableMap(written));
    }

    /**
     * @return The name of the binding annotation
     */
    public String name() {
        return name;
    }

    /**
     * @return The members that bind, spelt as Micronaut records them
     */
    public Map<String, Object> members() {
        return members;
    }

    /**
     * The binding as a value of its annotation, as the processor records it: the members that bind, spelt as
     * Micronaut records them, and the members excluded from it listed under
     * {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}.
     *
     * <p>Every call builds a value of its own, whose arrays are copies: what is done to them changes nothing of the
     * binding.</p>
     *
     * @return The value
     */
    public AnnotationValue<?> toAnnotationValue() {
        Map<CharSequence, Object> written = new LinkedHashMap<>(members.size() + 1);
        members.forEach((member, value) -> written.put(member, copyOf(value)));
        // the excluded members are listed for whatever the binding is compared with, which may be the value of an
        // annotation instance that carries all of its members and says nothing of which bind
        if (!nonBinding.isEmpty()) {
            written.put(AnnotationUtil.NON_BINDING_ATTRIBUTE, nonBinding.toArray(String[]::new));
        }
        return AnnotationValue.builder(name).members(written).build();
    }

    /**
     * Whether this and another binding are the same binding, compared by the members neither of them excludes.
     *
     * @param other The other binding
     * @return Whether the two are the same binding
     */
    public boolean matches(BindingValue other) {
        if (!name.equals(other.name)) {
            return false;
        }
        if (nonBinding.equals(other.nonBinding)) {
            return compared.equals(other.compared);
        }
        // either side may know of an excluded member the other does not list, and it is excluded from both
        Set<String> excluded = new TreeSet<>(nonBinding);
        excluded.addAll(other.nonBinding);
        return without(compared, excluded).equals(without(other.compared, excluded));
    }

    /**
     * Two bindings are equal when their names and the members they are compared by are, which is what the processor
     * tells two declarations of a binding apart by. The members excluded take no part.
     *
     * @param o The other object
     * @return Whether the two are equal
     */
    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || (o instanceof BindingValue other && name.equals(other.name) && compared.equals(other.compared));
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + compared.hashCode();
    }

    @Override
    public String toString() {
        return "@" + name + compared;
    }

    private static Map<String, Object> without(Map<String, Object> members, Set<String> excluded) {
        Map<String, Object> kept = new LinkedHashMap<>(members);
        kept.keySet().removeAll(excluded);
        return kept;
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
        Map<CharSequence, Object> copies = new LinkedHashMap<>(values.size());
        values.forEach((member, value) -> copies.put(member, copyOf(value)));
        return AnnotationValue.builder(annotation.getAnnotationName()).members(copies).build();
    }

    /**
     * Tells whether a member value is the empty string, which is compared as the member left out.
     */
    private static boolean isEmptyString(@Nullable Object value) {
        return value instanceof String string && string.isEmpty();
    }

    /**
     * Spells a member value the way a binding is recorded: as Micronaut records it, an enum constant by its name and
     * a class as an {@link AnnotationClassValue}, with an annotation it holds reduced as a binding is. An array keeps
     * its type, which is what Micronaut writes the metadata of an annotation from.
     */
    private static Object writable(Object value,
                                   Function<String, ? extends Collection<String>> excluded,
                                   boolean withDefaults,
                                   boolean leaveOutUndefaultedEmptyStrings) {
        if (value instanceof AnnotationValue<?> annotation) {
            return of(annotation, excluded, true, withDefaults, leaveOutUndefaultedEmptyStrings).toAnnotationValue();
        }
        if (value instanceof AnnotationValue<?>[] annotations) {
            AnnotationValue<?>[] written = new AnnotationValue<?>[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                written[i] = of(annotations[i], excluded, true, withDefaults, leaveOutUndefaultedEmptyStrings)
                    .toAnnotationValue();
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
            return writable(expression.annotationValue(), excluded, withDefaults, leaveOutUndefaultedEmptyStrings);
        }
        return value;
    }

    /**
     * Brings a member value, spelt as it is recorded, into the one shape it is compared in. An annotation it holds
     * was reduced already, and is read as recorded.
     */
    private static Object normalize(Object value) {
        return switch (value) {
            case AnnotationValue<?> annotation -> ofRecorded(annotation);
            case Enum<?> constant -> constant.name();
            case Class<?> type -> new AnnotationClassValue<>(type.getName());
            case Object[] array -> {
                List<Object> elements = new ArrayList<>(array.length);
                for (Object element : array) {
                    elements.add(normalize(element));
                }
                yield Collections.unmodifiableList(elements);
            }
            case int[] array -> Arrays.stream(array).boxed().toList();
            case long[] array -> Arrays.stream(array).boxed().toList();
            case double[] array -> Arrays.stream(array).boxed().toList();
            case boolean[] array -> {
                List<Object> elements = new ArrayList<>(array.length);
                for (boolean element : array) {
                    elements.add(element);
                }
                yield elements;
            }
            case byte[] array -> {
                List<Object> elements = new ArrayList<>(array.length);
                for (byte element : array) {
                    elements.add(element);
                }
                yield elements;
            }
            case short[] array -> {
                List<Object> elements = new ArrayList<>(array.length);
                for (short element : array) {
                    elements.add(element);
                }
                yield elements;
            }
            case char[] array -> {
                List<Object> elements = new ArrayList<>(array.length);
                for (char element : array) {
                    elements.add(element);
                }
                yield elements;
            }
            case float[] array -> {
                List<Object> elements = new ArrayList<>(array.length);
                for (float element : array) {
                    elements.add(element);
                }
                yield elements;
            }
            default -> value;
        };
    }
}
