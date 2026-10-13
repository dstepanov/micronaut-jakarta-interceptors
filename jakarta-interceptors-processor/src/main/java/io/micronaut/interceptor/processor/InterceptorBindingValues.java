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
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.internal.metadata.BindingValue;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
        Set<BindingValue> bindings = new LinkedHashSet<>(names.size());
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
     * <p>Micronaut records a member excluded from a binding only on an occurrence of the annotation that supplied a
     * value for that member, so an occurrence that left every excluded member to its default records none of them
     * and would otherwise be compared by values that take no part in the binding. The members excluded from the
     * annotation type are therefore supplied as well, so that two occurrences are compared by the same members
     * wherever either of them is read from.</p>
     *
     * <p>Reading the defaults off the annotation type instead of the occurrence does not work for every language:
     * what the visitor context offers for it answers all of them for Java, none for Kotlin, and for Groovy only the
     * ones that are a single constant.</p>
     *
     * @param annotation The binding annotation
     * @param excluded   The members excluded from a binding annotation, by its name
     * @return The binding
     */
    public static BindingValue of(AnnotationValue<?> annotation, ExcludedMembers excluded) {
        return BindingValue.reduce(annotation, excluded::of);
    }

    /**
     * Reads the members excluded from a binding annotation off the annotation type itself, remembering what it read:
     * the same annotation is reached as often as it is declared, and it is the same annotation every time.
     *
     * <p>Javac and the Groovy compiler answer the members of an annotation as its methods. KSP answers them as its
     * properties and no methods, a Java annotation on the classpath included, so those are read where there are no
     * methods.</p>
     *
     * @param context The visitor context, which resolves the annotation types
     * @return The excluded members of any binding annotation the compilation can see
     */
    public static ExcludedMembers excludedMembersOf(VisitorContext context) {
        Map<String, List<String>> read = new HashMap<>();
        return name -> read.computeIfAbsent(name, annotationName -> {
            ClassElement annotationType = context.getClassElement(annotationName).orElse(null);
            if (annotationType == null) {
                return List.of();
            }
            List<? extends Element> members = annotationType.getEnclosedElements(ElementQuery.ALL_METHODS);
            if (members.isEmpty()) {
                members = annotationType.getBeanProperties();
            }
            return members.stream()
                .filter(member -> member.hasAnnotation(JakartaInterceptors.NONBINDING)
                    || member.hasAnnotation(NonBinding.class))
                .map(Element::getName)
                .toList();
        });
    }

    /**
     * The members excluded from a binding annotation, answered by the name of the annotation.
     *
     * <p>What an occurrence of an annotation records is not the whole of it: Micronaut records an excluded member
     * only where a value was supplied for it. The complete set is a property of the annotation type, which the
     * compilation can read and the runtime cannot, so it is supplied to wherever a binding is reduced.</p>
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
