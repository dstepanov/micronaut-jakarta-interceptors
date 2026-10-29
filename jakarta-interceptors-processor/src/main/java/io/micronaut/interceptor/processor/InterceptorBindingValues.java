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

import io.micronaut.context.annotation.NonBinding;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationUtil;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ast.AnnotationElement;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.internal.metadata.BindingValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;

import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the interceptor bindings of an element and reduces each of them to what it is compared by.
 *
 * <p>An interceptor applies to an element when every binding annotation the interceptor declares is also declared
 * by the element, with the same member values. A member excluded from the binding, which is what
 * {@code jakarta.enterprise.util.Nonbinding} does, is left out of the comparison, and the values an annotation
 * defaults to are filled in first, so that {@code @Logged} and {@code @Logged(level = "INFO")} are the same binding
 * when {@code "INFO"} is the default.</p>
 *
 * <p>Which members take part in a binding does not depend on the running application, so it is worked out here: each
 * binding is written out on the element as the value of its annotation with the defaults filled in, the excluded
 * members left out and listed under {@value AnnotationUtil#NON_BINDING_ATTRIBUTE}, and an enum constant or a class
 * spelt as Micronaut records it, by its name. A binding is reduced by {@link BindingValue}, which is what the
 * runtime reads the recorded values with too, so that a conflict between two declarations of a binding is decided at
 * compilation time by exactly what decides at runtime whether an interceptor is bound.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class InterceptorBindingValues {

    private InterceptorBindingValues() {
    }

    /**
     * Reads the bindings an element declares, with the members excluded from each of them left out however the
     * element declared it.
     *
     * @param annotationMetadata The metadata of the element
     * @param excluded           The members excluded from a binding annotation, by its name
     * @return The bindings
     */
    public static Set<BindingValue> of(AnnotationMetadata annotationMetadata, ExcludedMembers excluded) {
        List<String> names =
            annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptorSupport.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return Set.of();
        }
        Set<BindingValue> bindings = CollectionUtils.newLinkedHashSet(names.size());
        for (String name : names) {
            // a repeatable binding binds by every occurrence of it the element carries, each of which is a binding
            // of its own: an interceptor declaring one of them is bound to the element, and one declaring an
            // occurrence the element does not carry is not. Reading the annotation by name answers with the first
            // occurrence alone, which made the order they were declared in decide what an element binds by
            List<AnnotationValue<Annotation>> occurrences = annotationMetadata.getAnnotationValuesByName(name);
            if (occurrences.isEmpty()) {
                annotationMetadata.findAnnotation(name)
                    .map(annotation -> of(annotation, excluded))
                    .ifPresent(bindings::add);
                continue;
            }
            for (AnnotationValue<Annotation> occurrence : occurrences) {
                bindings.add(of(occurrence, excluded));
            }
        }
        return bindings;
    }

    /**
     * Reduces one binding annotation to what it binds by.
     *
     * <p>Micronaut records every member excluded from a binding on each occurrence of the annotation, whether or not
     * the occurrence supplied a value for it, and those are what the occurrence is reduced by. An annotation held by
     * a member of the binding is recorded without them, so the members excluded from its type are supplied, and two
     * occurrences are compared by the same members wherever either of them is read from.</p>
     *
     * <p>Reading the defaults off the annotation type instead of the occurrence does not work for every language:
     * what the visitor context offers for it answers all of them for Java, none for Kotlin, and for Groovy only the
     * ones that are a single constant.</p>
     *
     * @param annotation The binding annotation
     * @param excluded   The members excluded from an annotation a member holds, by its name
     * @return The binding
     */
    public static BindingValue of(AnnotationValue<?> annotation, ExcludedMembers excluded) {
        return BindingValue.reduce(annotation, excluded::of);
    }

    /**
     * Reads what the bindings of an element are compared by.
     *
     * <p>Comparing an interceptor with an element it might intercept is comparing the members of their binding
     * annotations, the ones they default to filled in and the ones excluded from the binding left out. Which
     * members those are does not depend on the running application, so it is worked out here, and each binding is
     * written out as the value of its annotation that the runtime compares.</p>
     *
     * <p>A binding a member declares replaces the one of the same type its class declares - the whole of it, so a
     * member the declaration leaves to its default takes the default rather than the value the class gives it. The
     * metadata Micronaut hands out for a method merges the two member by member instead, so the class and the
     * member are read apart: the bindings of the class first, and the ones the member's own metadata holds written
     * over them by type. A binding declared on another annotation is one of the element's as well, which is why
     * the bindings are looked for by their stereotype rather than among the annotations the element declares
     * itself.</p>
     *
     * <p>A repeatable binding is as many bindings as the element carries occurrences of it, so the bindings are kept
     * by type as a list rather than as one: what a member declares of a type replaces every occurrence of that type
     * its class declares, and the occurrences of a type the member says nothing about are all inherited.</p>
     *
     * <p>The members excluded from a binding are the ones Micronaut records on each occurrence, every member the
     * annotation type excludes whether or not the occurrence declares a value for it, so an interceptor and the
     * element it intercepts agree on which members are compared. An annotation held by a member of a binding is
     * recorded without them, and has them read off its type.</p>
     *
     * @param element The element
     * @param owner   The class the element belongs to, or {@code null} when the element is the class
     * @param context The visitor context, which resolves the binding annotation types
     * @return The bindings, in a stable order: by the name of the annotation, the occurrences of one in the order
     * they are declared in
     */
    public static List<BindingValue> bindingsOf(Element element,
                                                @Nullable ClassElement owner,
                                                VisitorContext context) {
        ExcludedMembers excluded = excludedMembersOf(context);
        Map<String, List<BindingValue>> bindings = new LinkedHashMap<>();
        if (owner != null) {
            groupByType(of(owner.getAnnotationMetadata(), excluded), bindings);
        }
        AnnotationMetadata own = InterceptorClassScanner.ownMetadataOf(element);
        Map<String, List<BindingValue>> declared = new LinkedHashMap<>();
        groupByType(of(own, excluded), declared);
        if (owner != null) {
            // the metadata of a method also holds the annotations of the method it overrides. A binding declared
            // on an overridden method is not a binding of the override, so only the ones the member declares
            // itself, or that an annotation it declares carries, are kept
            Set<String> ownAnnotations = new HashSet<>(own.getDeclaredAnnotationNames());
            ownAnnotations.addAll(own.getDeclaredStereotypeAnnotationNames());
            // a repeatable binding is declared through its container, and is found by its occurrences
            declared.keySet().removeIf(name -> !ownAnnotations.contains(name)
                && own.getDeclaredAnnotationValuesByName(name).isEmpty());
        }
        bindings.putAll(declared);
        return bindings.values()
            .stream()
            .flatMap(List::stream)
            .sorted(Comparator.comparing(BindingValue::name))
            .toList();
    }

    /**
     * The bindings as they are written out: the value of each binding annotation, named by the annotation.
     *
     * @param bindings The bindings
     * @return The values
     */
    public static AnnotationValue<?>[] written(List<BindingValue> bindings) {
        return bindings.stream()
            .map(BindingValue::toAnnotationValue)
            .toArray(AnnotationValue<?>[]::new);
    }

    private static void groupByType(Set<BindingValue> read,
                                    Map<String, List<BindingValue>> into) {
        for (BindingValue binding : read) {
            into.computeIfAbsent(binding.name(), name -> new ArrayList<>()).add(binding);
        }
    }

    /**
     * Reads the members excluded from a binding annotation off the annotation type itself, remembering what it read:
     * the same annotation is reached as often as it is declared, and it is the same annotation every time.
     *
     * <p>The members are those Micronaut answers for the annotation type under every compiler alike: javac and the
     * Groovy compiler answer them as methods, KSP as properties.</p>
     *
     * @param context The visitor context, which resolves the annotation types
     * @return The excluded members of any binding annotation the compilation can see
     */
    public static ExcludedMembers excludedMembersOf(VisitorContext context) {
        Map<String, List<String>> read = new HashMap<>();
        return name -> read.computeIfAbsent(name, annotationName -> {
            if (!(context.getClassElement(annotationName).orElse(null) instanceof AnnotationElement annotationType)) {
                return List.of();
            }
            return annotationType.getMembers().stream()
                .filter(member -> member.hasAnnotation(JakartaInterceptors.NONBINDING)
                    || member.hasAnnotation(NonBinding.class))
                .map(Element::getName)
                .toList();
        });
    }

    /**
     * The members excluded from a binding annotation, answered by the name of the annotation.
     *
     * <p>Micronaut records them on every occurrence of an annotation an element declares, but not on an annotation
     * held by a member of another. For that one the set is a property of the annotation type, which the compilation
     * can read and the runtime cannot, so it is supplied to wherever a binding is reduced.</p>
     */
    public interface ExcludedMembers {

        /**
         * The members excluded from a binding annotation.
         *
         * @param annotationName The name of the annotation
         * @return The names of the excluded members, which is empty for an annotation excluding none
         */
        List<String> of(String annotationName);
    }
}
