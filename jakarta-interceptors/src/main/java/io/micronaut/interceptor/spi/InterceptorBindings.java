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
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanType;
import io.micronaut.interceptor.internal.metadata.BindingValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import io.micronaut.interceptor.internal.runtime.BoundInterceptorQualifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
 * <p>Two bindings are compared by every member that binds, once each of them has been brought into one shape:</p>
 * <ul>
 *     <li>the members the annotation defaults to are filled in from the defaults the value carries, so that
 *     {@code @Logged} and {@code @Logged(level = "INFO")} are the same binding where {@code "INFO"} is the default.
 *     A value the processor recorded is read as it was recorded, with its defaults filled in already, and so reads
 *     the same however the defaults Micronaut registers for its annotation change while the application runs;</li>
 *     <li>the members a binding lists as excluded from it under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE} are
 *     left out of both sides, whichever of the two lists them;</li>
 *     <li>a member that is an empty string is compared as if it were left out, which is the one default Micronaut
 *     does not record for an annotation compiled from Java or Kotlin;</li>
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

    private final List<BindingValue> bindings;
    private volatile @Nullable List<AnnotationValue<?>> values;

    private InterceptorBindings(List<BindingValue> bindings) {
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
        return recorded == null ? NONE : ofRecorded(recorded.getAnnotations(BINDINGS));
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
        return interception == null ? NONE : ofRecorded(interception.getAnnotations(BINDINGS));
    }

    /**
     * Reads a set of bindings from the values of the binding annotations, which may have been read from the
     * metadata of an element or built from annotation instances. The members a value leaves out are filled in from
     * the defaults it carries.
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
        return read(values, BindingValue::of);
    }

    /**
     * Reads a set of bindings the processor recorded, as they were recorded: their defaults were filled in when they
     * were, and are not read again.
     *
     * @param recorded The bindings as recorded
     * @return The bindings
     */
    @Internal
    public static InterceptorBindings ofRecorded(Collection<? extends AnnotationValue<?>> recorded) {
        return read(recorded, BindingValue::ofRecorded);
    }

    private static InterceptorBindings read(Collection<? extends AnnotationValue<?>> values,
                                            Function<AnnotationValue<?>, BindingValue> reader) {
        if (values.isEmpty()) {
            return NONE;
        }
        List<BindingValue> read = new ArrayList<>(values.size());
        for (AnnotationValue<?> value : values) {
            BindingValue binding = reader.apply(value);
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
     * The bindings as values of their binding annotations, one for each binding, in the order they were read.
     *
     * <p>Each holds the members that bind, with the ones the annotation defaults to filled in and an enum constant or
     * a class spelt as Micronaut records it, and lists the members excluded from the binding under
     * {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}, as the processor records a binding. The excluded members
     * themselves are not held: they take no part in what the binding is. Two values that are the same binding are
     * one, so a binding declared twice is here once.</p>
     *
     * <p>This is what a module that describes the bindings of an interceptor - as Contexts and Dependency Injection
     * does, answering {@code Interceptor.getInterceptorBindings()} - reads them from, so that what it describes is
     * what the interceptor is bound by. Read back through {@link #ofValues(Collection)}, the values are these
     * bindings again.</p>
     *
     * @return The values, unmodifiable
     */
    public List<AnnotationValue<?>> values() {
        List<AnnotationValue<?>> read = values;
        if (read == null) {
            // two threads may build the list at once, and build equal lists
            List<AnnotationValue<?>> built = new ArrayList<>(bindings.size());
            for (BindingValue binding : bindings) {
                built.add(binding.toAnnotationValue());
            }
            read = Collections.unmodifiableList(built);
            values = read;
        }
        return read;
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
        for (BindingValue binding : bindings) {
            if (!point.contains(binding)) {
                return false;
            }
        }
        return true;
    }

    private boolean contains(BindingValue binding) {
        for (BindingValue candidate : bindings) {
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
        for (BindingValue binding : bindings) {
            if (!other.contains(binding)) {
                return false;
            }
        }
        for (BindingValue binding : other.bindings) {
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
        for (BindingValue binding : bindings) {
            names.add(binding.name());
        }
        return names.hashCode();
    }

    @Override
    public String toString() {
        return bindings.toString();
    }
}
