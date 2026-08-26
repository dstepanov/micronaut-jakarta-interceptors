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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.interceptor.runtime.InterceptorBindingValues;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Finds a binding annotation that reaches an element along two paths, carrying different member values.
 *
 * <p>A binding annotation may be declared on another binding annotation, which is what makes the second bind
 * everything the first does. Two annotations declared together may each carry the same binding with a different
 * value, and the element then has no one answer to what that binding binds by. Java does not prevent it: the two
 * declarations are of different annotation types, and only the binding they have in common conflicts. The
 * specification asks for a definition error, which is what this reports.</p>
 *
 * <p>A binding an element declares itself is the answer for that element, whatever the annotations it declares
 * carry: declaring {@code @Ball(requiresBall = true)} on a class that is also annotated with something carrying
 * {@code @Ball} is how the value of a binding is chosen, not a conflict. Only two paths that both merely pass a
 * binding along, with neither declared where they meet, disagree.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class BindingConflicts {

    private BindingConflicts() {
    }

    /**
     * Looks for a binding that reaches an element twice with different values.
     *
     * <p>What an element declares itself is read from the element; the annotations it declares are followed from
     * there, so a binding reached through a superclass is found on the superclass rather than here.</p>
     *
     * @param element The element the bindings are declared on
     * @param context The visitor context, which resolves the annotation types that are followed
     * @return The name of the conflicting binding annotation, or {@code null} when there is none
     */
    public static @Nullable String conflictOf(Element element, VisitorContext context) {
        Set<String> conflicts = resolve(element.getAnnotationMetadata(), context, new HashMap<>(), new HashSet<>())
            .conflicts();
        return conflicts.isEmpty() ? null : conflicts.iterator().next();
    }

    /**
     * Reads the bindings in effect at one element or annotation type, and the ones that disagree there.
     *
     * @param metadata   The metadata to read
     * @param context    The visitor context
     * @param resolved   What has already been worked out for an annotation type, which is the same wherever it is
     *                   declared
     * @param resolving  The annotation types being worked out further up, which is what ends a cycle
     */
    private static Bindings resolve(AnnotationMetadata metadata,
                                    VisitorContext context,
                                    Map<String, Bindings> resolved,
                                    Set<String> resolving) {
        // what the element says itself, which is the answer for the element
        Map<String, InterceptorBindingValues.Binding> declared = new LinkedHashMap<>();
        // what the annotations of the element pass along, which is only the answer where the element is silent
        Map<String, InterceptorBindingValues.Binding> passed = new LinkedHashMap<>();
        Set<String> conflicts = new LinkedHashSet<>();
        Set<String> declaredNames = metadata.getDeclaredAnnotationNames();
        for (String name : metadata.getAnnotationNames()) {
            if (JakartaInterceptors.INTERCEPTOR_BINDING.equals(name)) {
                // a binding annotation names itself among its bindings; it is not one of its own
                continue;
            }
            ClassElement type = context.getClassElement(name).orElse(null);
            // an annotation carrying no binding, however far it is followed, leads nowhere: this leaves out the
            // annotations of the platform, which every annotation type is declared with, along with the rest
            if (type == null || !type.hasStereotype(JakartaInterceptors.INTERCEPTOR_BINDING)) {
                continue;
            }
            if (declaredNames.contains(name) && type.hasDeclaredAnnotation(JakartaInterceptors.INTERCEPTOR_BINDING)) {
                AnnotationValue<?> value = metadata.findAnnotation(name).orElse(null);
                if (value != null) {
                    declared.put(name, InterceptorBindingValues.of(value));
                }
            }
            Bindings carried = of(name, type, context, resolved, resolving);
            conflicts.addAll(carried.conflicts());
            carried.bindings().forEach((carriedName, binding) -> {
                InterceptorBindingValues.Binding existing = passed.putIfAbsent(carriedName, binding);
                if (existing != null && !existing.equals(binding)) {
                    conflicts.add(carriedName);
                }
            });
        }
        // a binding the element declares is what that binding is here, so two annotations passing it along with
        // different values no longer disagree: the element has already said which value it has
        conflicts.removeAll(declared.keySet());
        Map<String, InterceptorBindingValues.Binding> bindings = new LinkedHashMap<>(passed);
        bindings.putAll(declared);
        return new Bindings(bindings, conflicts);
    }

    /**
     * The bindings one annotation type passes along to whatever it is declared on.
     */
    private static Bindings of(String name,
                               ClassElement type,
                               VisitorContext context,
                               Map<String, Bindings> resolved,
                               Set<String> resolving) {
        Bindings known = resolved.get(name);
        if (known != null) {
            return known;
        }
        if (!resolving.add(name)) {
            // an annotation declared on itself, directly or through others; it passes nothing further along
            return Bindings.EMPTY;
        }
        try {
            Bindings bindings = resolve(type.getAnnotationMetadata(), context, resolved, resolving);
            resolved.put(name, bindings);
            return bindings;
        } finally {
            resolving.remove(name);
        }
    }

    /**
     * The bindings in effect somewhere, and the ones that reached it disagreeing with themselves.
     *
     * @param bindings  The bindings, by annotation name
     * @param conflicts The names of the bindings that disagree
     */
    private record Bindings(Map<String, InterceptorBindingValues.Binding> bindings, Set<String> conflicts) {

        private static final Bindings EMPTY = new Bindings(Map.of(), Set.of());
    }
}
