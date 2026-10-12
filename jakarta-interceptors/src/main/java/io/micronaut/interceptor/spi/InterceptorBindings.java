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
package io.micronaut.interceptor.spi;

import io.micronaut.context.Qualifier;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanType;
import io.micronaut.inject.annotation.AnnotationMetadataSupport;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import io.micronaut.interceptor.internal.runtime.BoundInterceptorQualifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The interceptor bindings of an interceptor class or of an interception point, as what they are compared by.
 *
 * <p>The specification binds an interceptor to an element when every binding annotation the interceptor declares
 * is declared by the element as well, with the same member values, the members excluded from the binding with
 * {@code jakarta.enterprise.util.Nonbinding} left out. The processor records the bindings of both on them as the
 * values of the binding annotations, with the members they default to filled in and the excluded ones left out, and
 * this is where the two are compared - by the chains of this module, and by any other module that has to answer the
 * same question, as Contexts and Dependency Injection does when its bean manager is asked for the interceptors of a
 * set of bindings. Comparing them here rather than again there is what keeps the two answers the same.</p>
 *
 * <p>Two bindings are compared with {@link AnnotationValue#matches(AnnotationValue)}, which compares every member,
 * once each of them has been brought into one shape:</p>
 * <ul>
 *     <li>the members the annotation defaults to are filled in, from the value itself and from the defaults
 *     Micronaut registered for the annotation, so that {@code @Logged} and {@code @Logged(level = "INFO")} are the
 *     same binding where {@code "INFO"} is the default;</li>
 *     <li>the members a binding lists as excluded from it under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE} are
 *     left out of both sides, whichever of the two lists them;</li>
 *     <li>an empty string Micronaut recorded no default for is left out, which is the one default it does not
 *     record for an annotation compiled from Java or Kotlin;</li>
 *     <li>an enum constant is its name, a class is an {@link AnnotationClassValue}, an array is the list of its
 *     elements and an annotation held by a member is compared exactly as a binding is, so that a value read from
 *     compiled metadata and one built from an annotation instance compare alike.</li>
 * </ul>
 *
 * <p>{@link AnnotationValue#equals(Object)} is not used: it passes over a member the other value does not have.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public final class InterceptorBindings {

    private static final String BINDINGS = "bindings";
    private static final InterceptorBindings NONE = new InterceptorBindings(List.of());

    private final List<Binding> bindings;

    private InterceptorBindings(List<Binding> bindings) {
        this.bindings = bindings;
    }

    /**
     * Reads the bindings an interceptor class declares, as the processor recorded them on its definition.
     *
     * @param interceptor The bean of the interceptor class
     * @return The bindings, none where the class declares none or is not an interceptor class
     */
    public static InterceptorBindings of(BeanType<?> interceptor) {
        AnnotationValue<JakartaInterceptorMethods> recorded =
            interceptor.getAnnotation(JakartaInterceptorMethods.class);
        return recorded == null ? NONE : ofValues(recorded.getAnnotations(BINDINGS));
    }

    /**
     * Reads the bindings of an intercepted element - a class, a constructor, a method or the member of a factory -
     * as the processor recorded them on it.
     *
     * @param metadata The annotation metadata of the element
     * @return The bindings, none where the element is bound by none
     */
    public static InterceptorBindings ofInterceptionPoint(AnnotationMetadata metadata) {
        AnnotationValue<JakartaInterception> interception = metadata.getAnnotation(JakartaInterception.class);
        return interception == null ? NONE : ofValues(interception.getAnnotations(BINDINGS));
    }

    /**
     * Reads a set of bindings from the values of the binding annotations, which may have been recorded at
     * compilation time or built from annotation instances.
     *
     * <p>A value lists the members excluded from its binding under
     * {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}. Those are left out wherever it is compared, and so are the
     * ones the other side of the comparison lists; the processor lists every member the annotation type excludes,
     * so a value that lists none still compares by the members that bind.</p>
     *
     * <p>A repeatable binding is as many values as it has occurrences; two values that are the same binding are
     * one.</p>
     *
     * @param values The values of the binding annotations
     * @return The bindings
     */
    public static InterceptorBindings ofValues(Collection<? extends AnnotationValue<?>> values) {
        if (values.isEmpty()) {
            return NONE;
        }
        List<Binding> read = new ArrayList<>(values.size());
        for (AnnotationValue<?> value : values) {
            Binding binding = Binding.of(value);
            if (read.stream().noneMatch(binding::matches)) {
                read.add(binding);
            }
        }
        return new InterceptorBindings(Collections.unmodifiableList(read));
    }

    /**
     * A qualifier that selects, among the beans of interceptor classes, the ones bound to an interception point.
     *
     * @param point The bindings of the interception point
     * @param <T>   The type the beans are looked up by
     * @return The qualifier, which compares by the bindings of the point
     */
    public static <T> Qualifier<T> boundTo(InterceptorBindings point) {
        return new BoundInterceptorQualifier<>(point);
    }

    /**
     * Tells whether there are no bindings at all, which an interceptor class or an element that is bound by nothing
     * has.
     *
     * @return Whether there are no bindings
     */
    public boolean isEmpty() {
        return bindings.isEmpty();
    }

    /**
     * Tells whether an interceptor declaring these bindings is bound to an interception point declaring the given
     * ones: every one of these is one of the point's. The point may declare more.
     *
     * <p>An interceptor declaring no binding is bound to nothing: it applies only where an element names it.</p>
     *
     * @param point The bindings of the interception point
     * @return Whether the interceptor is bound to the point
     */
    public boolean isBoundBy(InterceptorBindings point) {
        if (bindings.isEmpty()) {
            return false;
        }
        for (Binding binding : bindings) {
            if (!point.contains(binding)) {
                return false;
            }
        }
        return true;
    }

    private boolean contains(Binding binding) {
        for (Binding candidate : bindings) {
            if (candidate.matches(binding)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Two sets of bindings are equal when each binding of either is a binding of the other.
     *
     * @param o The other object
     * @return Whether the two are the same bindings
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InterceptorBindings other) || bindings.size() != other.bindings.size()) {
            return false;
        }
        for (Binding binding : bindings) {
            if (!other.contains(binding)) {
                return false;
            }
        }
        for (Binding binding : other.bindings) {
            if (!contains(binding)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Hashed by the names of the binding annotations alone, which two equal sets of bindings share whatever their
     * members hold.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        Set<String> names = new HashSet<>();
        for (Binding binding : bindings) {
            names.add(binding.name());
        }
        return names.hashCode();
    }

    @Override
    public String toString() {
        return bindings.stream().map(binding -> binding.value().toString()).toList().toString();
    }

    /**
     * One binding, in the shape it is compared in.
     *
     * @param name       The name of the binding annotation
     * @param nonBinding The members the value lists as excluded from the binding
     * @param members    The members that bind, normalized
     * @param value      The members as an annotation value without defaults, which is what is compared
     */
    private record Binding(String name, Set<String> nonBinding, Map<String, Object> members, AnnotationValue<?> value) {

        static Binding of(AnnotationValue<?> annotation) {
            String name = annotation.getAnnotationName();
            Map<CharSequence, Object> registered = AnnotationMetadataSupport.getDefaultValues(name);
            Set<String> registeredNames = new HashSet<>();
            Map<String, Object> completed = new LinkedHashMap<>();
            registered.forEach((member, value) -> {
                registeredNames.add(member.toString());
                completed.put(member.toString(), value);
            });
            Map<CharSequence, Object> defaults = annotation.getDefaultValues();
            if (defaults != null) {
                defaults.forEach((member, value) -> completed.put(member.toString(), value));
            }
            annotation.getValues().forEach((member, value) -> completed.put(member.toString(), value));
            Set<String> nonBinding = new LinkedHashSet<>(Arrays.asList(
                annotation.stringValues(AnnotationUtil.NON_BINDING_ATTRIBUTE)));
            completed.remove(AnnotationUtil.NON_BINDING_ATTRIBUTE);
            completed.remove(AnnotationUtil.STEREOTYPES_MEMBER);
            completed.keySet().removeAll(nonBinding);
            // an empty string declared for a member with no registered default reads the same as the member left
            // out, which is what an annotation compiled from Java or Kotlin defaulting to "" records
            completed.entrySet().removeIf(entry -> "".equals(entry.getValue())
                && !registeredNames.contains(entry.getKey()));
            Map<String, Object> members = new LinkedHashMap<>(completed.size());
            completed.forEach((member, value) -> members.put(member, normalize(value)));
            return new Binding(name, Set.copyOf(nonBinding), members, valueOf(name, members, Set.of()));
        }

        boolean matches(Binding other) {
            if (!name.equals(other.name)) {
                return false;
            }
            if (nonBinding.equals(other.nonBinding)) {
                return value.matches(other.value);
            }
            // either side may know of an excluded member the other does not list, and it is excluded from both
            Set<String> excluded = new HashSet<>(nonBinding);
            excluded.addAll(other.nonBinding);
            return valueOf(name, members, excluded).matches(valueOf(name, other.members, excluded));
        }

        /**
         * The value compared, which carries no defaults of its own: they were filled in already, and the ones
         * Micronaut registered would otherwise bring back the members left out.
         */
        private static AnnotationValue<?> valueOf(String name, Map<String, Object> members, Set<String> excluded) {
            Map<CharSequence, Object> compared = new LinkedHashMap<>(members.size());
            members.forEach((member, value) -> {
                if (!excluded.contains(member)) {
                    compared.put(member, value);
                }
            });
            return new AnnotationValue<>(name, compared, Collections.emptyMap());
        }

        /**
         * Brings a member value into the one shape it is compared in, whether it was read from compiled metadata
         * or built from an annotation instance. The array types are listed out rather than read through
         * {@code java.lang.reflect.Array}, which keeps the comparison free of reflection.
         */
        private static Object normalize(Object value) {
            return switch (value) {
                case AnnotationValue<?> annotation -> Nested.of(annotation);
                case Enum<?> constant -> constant.name();
                case Class<?> type -> new AnnotationClassValue<>(type.getName());
                case Object[] array -> {
                    List<Object> elements = new ArrayList<>(array.length);
                    for (Object element : array) {
                        elements.add(normalize(element));
                    }
                    yield elements;
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

    /**
     * An annotation held by a member of a binding, compared by its name and every member that binds. A record
     * rather than an annotation value, since the elements of an array are compared with their own
     * {@code equals}, which for an annotation value passes over a member the other one does not have.
     *
     * @param name    The name of the annotation
     * @param members The members that bind, normalized
     */
    private record Nested(String name, Map<String, Object> members) {

        static Nested of(AnnotationValue<?> annotation) {
            Binding binding = Binding.of(annotation);
            return new Nested(binding.name(), binding.members());
        }
    }
}
