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
import io.micronaut.context.Qualifier;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanType;
import io.micronaut.inject.annotation.AnnotationMetadataHierarchy;
import io.micronaut.interceptor.internal.metadata.BindingValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import io.micronaut.interceptor.internal.runtime.BoundInterceptorQualifier;
import io.micronaut.interceptor.internal.runtime.InterceptorChainResolver;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
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

    private static final InterceptorBindings NONE = new InterceptorBindings(List.of());

    private final List<BindingValue> bindings;
    private final int hash;
    // the bindings in the shape they are compared in, and as an interception point Micronaut selects the
    // interceptors of with its exact rule: every binding of an interceptor applies at the point
    private final List<AnnotationValue<?>> matched;
    private final ExactInterceptorBindingQualifier<?> point;

    private InterceptorBindings(List<BindingValue> bindings) {
        this.bindings = bindings;
        Set<String> names = new HashSet<>();
        List<AnnotationValue<?>> values = new ArrayList<>(bindings.size());
        for (BindingValue binding : bindings) {
            names.add(binding.name());
            values.add(binding.matched());
        }
        this.hash = names.hashCode();
        this.matched = values;
        // the kind takes no part in comparing occurrences, which carry no kind of their own
        this.point = ExactInterceptorBindingQualifier.ofOccurrences(values, InterceptorKind.AROUND);
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
        return recorded == null ? NONE : ofRecorded(recorded.getAnnotations(JakartaInterceptorMethods.BINDINGS));
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
        return interception == null ? NONE : ofRecorded(interception.getAnnotations(JakartaInterception.BINDINGS));
    }

    /**
     * The declarations of the bindings the processor recorded on an intercepted element, with the members that do
     * not bind kept: what {@code getInterceptorBindingValues()} of an invocation of the element reports.
     *
     * <p>Which bindings they are, and how many occurrences of a repeatable one, is what the processor recorded on the
     * element: a binding only the method an element overrides declares, or that only the factory producing a bean
     * declares, is not one of them, although the metadata of the element holds it, and occurrences that differ only
     * in a member that does not bind are one binding. What was recorded holds only the members that bind, so each
     * binding is answered with the declaration it was reduced from: the nearest declaration of it that is the same
     * binding, the element's before its class's, and what was recorded where there is none.</p>
     *
     * @param metadata The annotation metadata of the element, as Micronaut compiled it
     * @return The declarations, empty where the element is bound by none
     */
    public static List<AnnotationValue<?>> declarations(AnnotationMetadata metadata) {
        AnnotationValue<JakartaInterception> interception = metadata.getAnnotation(JakartaInterception.class);
        List<AnnotationValue<Annotation>> recorded = interception == null
            ? List.of()
            : interception.getAnnotations(JakartaInterception.BINDINGS);
        if (recorded.isEmpty()) {
            return List.of();
        }
        List<AnnotationValue<?>> values = new ArrayList<>(recorded.size());
        for (AnnotationValue<Annotation> binding : recorded) {
            values.add(declarationOf(metadata, binding));
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * The declaration a recorded binding was reduced from: the nearest occurrence of its annotation that is the same
     * binding, read one layer of the metadata at a time and, within a layer, what the element declares itself before
     * what it carries otherwise.
     *
     * <p>The metadata of a method is a hierarchy of the metadata of its class and its own, and asked as a whole it
     * merges the members of a binding both declare, where the declaration of the method replaces the one of its
     * class whole; so the layers are read apart. The layer of a method also holds the annotations of the method it
     * overrides, which are not declarations of the method, so there an occurrence is taken only where the method
     * declares it, or an annotation the method declares carries it - as the processor reads the bindings of the
     * method. Within the layer of the class, an occurrence is taken only where it is the binding recorded.</p>
     */
    private static AnnotationValue<?> declarationOf(AnnotationMetadata metadata, AnnotationValue<?> recorded) {
        String name = recorded.getAnnotationName();
        BindingValue binding = BindingValue.ofRecorded(recorded);
        List<AnnotationMetadata> layers = nearestFirst(metadata);
        for (int i = 0; i < layers.size(); i++) {
            // the last layer is the class, and every one before it is the element's own
            boolean own = i < layers.size() - 1;
            for (AnnotationValue<?> occurrence : occurrencesOf(layers.get(i), name, own)) {
                if (BindingValue.of(occurrence).matches(binding)) {
                    return occurrence;
                }
            }
        }
        return recorded;
    }

    /**
     * The occurrences of an annotation one layer of the metadata holds, the ones the element declares itself first.
     * In a layer of the element's own, the occurrences of the method it overrides are left out: only what the layer
     * declares, and what an annotation it declares carries, is read.
     */
    private static List<AnnotationValue<?>> occurrencesOf(AnnotationMetadata layer, String name, boolean own) {
        List<AnnotationValue<?>> occurrences = new ArrayList<>(2);
        boolean carried = !own || layer.getDeclaredStereotypeAnnotationNames().contains(name);
        if (layer.findRepeatableAnnotation(name).isPresent()) {
            occurrences.addAll(layer.getDeclaredAnnotationValuesByName(name));
            if (carried) {
                occurrences.addAll(layer.getAnnotationValuesByName(name));
            }
        } else {
            layer.findDeclaredAnnotation(name).ifPresent(occurrences::add);
            if (carried) {
                layer.findAnnotation(name).ifPresent(occurrences::add);
            }
        }
        return occurrences;
    }

    /**
     * The layers of the metadata from the element itself to the root, its class.
     */
    private static List<AnnotationMetadata> nearestFirst(AnnotationMetadata metadata) {
        if (!(metadata instanceof AnnotationMetadataHierarchy hierarchy)) {
            return List.of(metadata);
        }
        List<AnnotationMetadata> layers = new ArrayList<>(2);
        // the hierarchy iterates from the root, the class, to the element itself
        for (AnnotationMetadata layer : hierarchy) {
            layers.addAll(0, nearestFirst(layer));
        }
        return layers;
    }

    /**
     * Reads a set of bindings from the values of the binding annotations, which may have been read from the
     * metadata of an element or built from annotation instances. The members a value leaves out are filled in from
     * the defaults it carries, {@link AnnotationValue#getDefaultValues()}, and from nothing else: the registry of
     * defaults Micronaut shares across the application is not asked again. A value read from metadata carries the
     * defaults that registry held when it was read, and the reflection module adds to the registry as it builds an
     * instance of an annotation - an empty string, which is compared as the member left out, and the defaults it
     * reads in the shapes it reads them in, which are compared as one - so a value read before and one read after
     * are the same bindings.
     *
     * <p>A value lists the members excluded from its binding under
     * {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}. Those are left out wherever it is compared, and so are the
     * ones the other side of the comparison lists. Micronaut lists every member the annotation type excludes on each
     * occurrence it records, and so does the processor on each binding it records, so a value that lists none, one
     * built from an annotation instance, still compares by the members that bind.</p>
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
     * <p>It compares the bindings and nothing else: which interceptor classes are enabled, and the order they are
     * invoked in, are left to whatever uses it. {@link #boundInterceptors(BeanContext, InterceptorBindings)} is the
     * selection the chains make, those included.</p>
     *
     * @param point The bindings of the interception point
     * @param <T>   The type the beans are looked up by
     * @return The qualifier, which compares by the bindings of the point
     */
    public static <T> Qualifier<T> boundTo(InterceptorBindings point) {
        return new BoundInterceptorQualifier<>(point);
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
     * reads, and whose bindings {@link #of(BeanType)} reads. The selection is made again for every call, since the
     * definitions a context knows of may still grow while its beans are first created.</p>
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
     * <p>Every call builds the values anew, and an array a value holds is the caller's: what is done to it changes
     * nothing of these bindings, nor of the values another call returns.</p>
     *
     * @return The values, unmodifiable
     */
    public List<AnnotationValue<?>> values() {
        List<AnnotationValue<?>> built = new ArrayList<>(bindings.size());
        for (BindingValue binding : bindings) {
            built.add(binding.toAnnotationValue());
        }
        return Collections.unmodifiableList(built);
    }

    /**
     * Tells whether an interceptor declaring these bindings is bound to an interception point declaring the given
     * ones: every one of these is one of the point's. The point may declare more.
     *
     * <p>An interceptor declaring no binding is bound to nothing: it applies only where an element names it.</p>
     *
     * <p>It is Micronaut's exact interceptor binding rule, {@link ExactInterceptorBindingQualifier#matches(Collection)},
     * applied to the bindings in the shape they are compared in.</p>
     *
     * @param point The bindings of the interception point
     * @return Whether the interceptor is bound to the point
     */
    public boolean isBoundBy(InterceptorBindings point) {
        return point.point.matches(matched);
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
