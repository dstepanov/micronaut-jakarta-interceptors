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

import io.micronaut.aop.ExactInterceptorBindingQualifier;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanType;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import io.micronaut.interceptor.internal.runtime.InterceptorChainResolver;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * <p>Two bindings are compared by Micronaut, {@link AnnotationValue#matches(AnnotationValue)}, on the values as they
 * were read: it fills in the members the annotation defaults to, so that {@code @Logged} and
 * {@code @Logged(level = "INFO")} are the same binding where {@code "INFO"} is the default; leaves out the members
 * either side lists under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}; compares an empty string with a member left
 * out with no default; and compares an enum constant with its name, a class with its class value and a primitive
 * array with its boxed elements, so that a value read from compiled metadata and one built from an annotation instance
 * compare alike. The processor records every member that binds, so the defaults Micronaut registers for an
 * annotation while the application runs change nothing of what a recorded binding is.</p>
 *
 * <p>{@link AnnotationValue#equals(Object)} is not used: it passes over a member the other value does not have.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public final class InterceptorBindings {

    private static final InterceptorBindings NONE = new InterceptorBindings(List.of());

    private final List<AnnotationValue<?>> bindings;
    private final int hash;
    // the bindings as an interception point Micronaut selects the interceptors of with its exact rule: every binding
    // of an interceptor applies at the point
    private final ExactInterceptorBindingQualifier<?> point;

    private InterceptorBindings(List<AnnotationValue<?>> bindings) {
        this.bindings = bindings;
        Set<String> names = new HashSet<>();
        for (AnnotationValue<?> binding : bindings) {
            names.add(binding.getAnnotationName());
        }
        this.hash = names.hashCode();
        // the kind takes no part in comparing occurrences, which carry no kind of their own
        this.point = ExactInterceptorBindingQualifier.ofOccurrences(bindings, InterceptorKind.AROUND);
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
        return recorded == null ? NONE : ofValues(recorded.getAnnotations(JakartaInterceptorMethods.BINDINGS));
    }

    /**
     * Reads a set of bindings from the values of the binding annotations, which may have been read from the
     * metadata of an element, as the processor recorded them, or built from annotation instances. The values are
     * kept as they are, but for the arrays their members hold, which are copied so that the caller's arrays stay the
     * caller's; they are compared by {@link AnnotationValue#matches(AnnotationValue)}.
     *
     * <p>A value lists the members excluded from its binding under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}.
     * Those are left out wherever it is compared, and so are the ones the other side of the comparison lists.
     * Micronaut lists every member the annotation type excludes on each occurrence it records, and so does the
     * processor on each binding it records, so a value that lists none, one built from an annotation instance, still
     * compares by the members that bind.</p>
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
        List<AnnotationValue<?>> read = new ArrayList<>(values.size());
        for (AnnotationValue<?> value : values) {
            if (read.stream().noneMatch(value::matches)) {
                read.add(snapshot(value));
            }
        }
        return new InterceptorBindings(Collections.unmodifiableList(read));
    }

    /**
     * The value with every array a member holds copied, at any depth, and everything else as it is: an
     * {@link AnnotationValue} keeps the arrays it is built with, and an array the caller changes afterwards must not
     * change which interceptors these bindings select. A value holding no array is kept itself.
     */
    private static AnnotationValue<?> snapshot(AnnotationValue<?> value) {
        Map<CharSequence, Object> members = value.getValues();
        Map<CharSequence, Object> copied = null;
        for (Map.Entry<CharSequence, Object> member : members.entrySet()) {
            Object snapshot = snapshotMember(member.getValue());
            if (snapshot != member.getValue()) {
                if (copied == null) {
                    copied = new LinkedHashMap<>(members);
                }
                copied.put(member.getKey(), snapshot);
            }
        }
        return copied == null
            ? value
            : new AnnotationValue<>(value.getAnnotationName(), copied, value.getDefaultValues());
    }

    /**
     * A member value with its arrays copied; listed out rather than read through {@code java.lang.reflect.Array}, so
     * that the interception stays free of the reflection of the platform.
     */
    private static Object snapshotMember(Object value) {
        return switch (value) {
            case AnnotationValue<?> annotation -> snapshot(annotation);
            case Object[] array -> {
                Object[] copy = array.clone();
                for (int i = 0; i < copy.length; i++) {
                    if (copy[i] instanceof AnnotationValue<?> annotation) {
                        copy[i] = snapshot(annotation);
                    }
                }
                yield copy;
            }
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

    /**
     * The interceptor classes of a context that are bound to an interception point, as the chains of this module
     * select them: the enabled interceptor classes every binding of which is a binding of the point, in the order a
     * chain invokes them in.
     *
     * <p>This is the selection itself rather than a part of it. It finds the interceptor classes the processor
     * indexed, keeps the ones {@link #isBoundBy(InterceptorBindings) bound} to the point, leaves out a class that
     * declares interceptor methods without declaring itself an interceptor class with
     * {@code jakarta.interceptor.Interceptor} and one a {@link BoundInterceptorEnablement} of the context does not
     * enable, and orders the rest by {@link InterceptorMethods#priorityOf(BeanDefinition) priority}, then by the
     * name of the class, with the ones an enablement lists after them in the order of the list. A module answering
     * the same question - as Contexts and Dependency Injection does when its bean manager is asked for the
     * interceptors of a set of bindings - asks it here, and the answer is the one the chains are built from, the
     * same resolver of the context answering both.</p>
     *
     * <p>Each definition is the one Micronaut generated from the interceptor class, which carries the executable
     * methods {@link InterceptorMethods#of(BeanDefinition, io.micronaut.interceptor.metadata.InterceptionKind)}
     * reads, and whose bindings {@link #of(BeanType)} reads. The selection is made again for every call; a chain keeps
     * the selection it was built from.</p>
     *
     * @param beanContext The bean context the interceptor classes are beans of
     * @param point       The bindings of the interception point
     * @return The definitions that describe the interceptor classes, none where the point is bound by nothing
     */
    public static List<BeanDefinition<?>> boundInterceptors(BeanContext beanContext, InterceptorBindings point) {
        if (point.isEmpty()) {
            return List.of();
        }
        return beanContext.getBean(InterceptorChainResolver.class).boundInterceptors(point);
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
     * <p>Each is the value as it was read: for the bindings of an interceptor class or an element, as the processor
     * recorded it, with the members that bind and the members excluded from the binding listed under
     * {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}. Two values that are the same binding are one, so a binding
     * declared twice is here once. Read back through {@link #ofValues(Collection)}, the values are these bindings
     * again.</p>
     *
     * @return The values, unmodifiable
     */
    public List<AnnotationValue<?>> values() {
        return bindings;
    }

    /**
     * Tells whether an interceptor declaring these bindings is bound to an interception point declaring the given
     * ones: every one of these is one of the point's. The point may declare more.
     *
     * <p>An interceptor declaring no binding is bound to nothing: it applies only where an element names it.</p>
     *
     * <p>It is Micronaut's exact interceptor binding rule, {@link ExactInterceptorBindingQualifier#matches(Collection)}.</p>
     *
     * @param point The bindings of the interception point
     * @return Whether the interceptor is bound to the point
     */
    public boolean isBoundBy(InterceptorBindings point) {
        return point.point.matches(bindings);
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
        return bindings.isEmpty() || (isBoundBy(other) && other.isBoundBy(this));
    }

    /**
     * Hashed by the names of the binding annotations alone, which two equal sets of bindings share whatever their
     * members hold.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return bindings.toString();
    }
}
