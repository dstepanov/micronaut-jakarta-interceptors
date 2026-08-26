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
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.interceptor.annotation.InterceptionKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds and validates the interceptor methods a class declares.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class InterceptorClassScanner {

    private static final Map<String, InterceptionKind> INTERCEPTOR_METHODS = Map.of(
        JakartaInterceptors.AROUND_INVOKE, InterceptionKind.AROUND_INVOKE,
        JakartaInterceptors.AROUND_TIMEOUT, InterceptionKind.AROUND_TIMEOUT,
        JakartaInterceptors.AROUND_CONSTRUCT, InterceptionKind.AROUND_CONSTRUCT,
        JakartaInterceptors.POST_CONSTRUCT, InterceptionKind.POST_CONSTRUCT,
        JakartaInterceptors.PRE_DESTROY, InterceptionKind.PRE_DESTROY
    );

    private InterceptorClassScanner() {
    }

    /**
     * Reads the interceptor methods of a class.
     *
     * <p>A lifecycle callback is an interceptor method only when it accepts an {@code InvocationContext}, which is
     * what tells the callback that interposes on the lifecycle of the objects an interceptor class intercepts from
     * the callback a class declares for its own lifecycle.</p>
     *
     * @param element The class
     * @return The model, whose {@link InterceptorClassModel#intercepts()} is {@code false} when the class declares
     * no interceptor method
     */
    public static InterceptorClassModel scan(ClassElement element) {
        Map<InterceptionKind, List<MethodElement>> methods = new EnumMap<>(InterceptionKind.class);
        // a class declares at most one interceptor method of a kind, but its superclasses declare their own, and
        // the specification invokes all of them with the most general superclass first
        List<String> hierarchy = hierarchyOf(element);
        Map<InterceptionKind, Map<String, MethodElement>> byDeclaringClass = new EnumMap<>(InterceptionKind.class);
        for (MethodElement method : element.getEnclosedElements(ElementQuery.ALL_METHODS)) {
            for (Map.Entry<String, InterceptionKind> entry : INTERCEPTOR_METHODS.entrySet()) {
                if (!method.hasDeclaredAnnotation(entry.getKey()) || !acceptsInvocationContext(method)) {
                    continue;
                }
                validate(element, method, entry.getKey(), entry.getValue());
                String declaringClass = method.getDeclaringType().getName();
                MethodElement existing = byDeclaringClass
                    .computeIfAbsent(entry.getValue(), kind -> new LinkedHashMap<>())
                    .putIfAbsent(declaringClass, method);
                if (existing != null && !existing.getName().equals(method.getName())) {
                    throw new ProcessingException(method, "The class [" + declaringClass + "] declares more than "
                        + "one @" + simpleName(entry.getKey()) + " method: [" + existing.getName() + "] and ["
                        + method.getName() + "]");
                }
            }
        }
        byDeclaringClass.forEach((kind, declarations) -> methods.put(kind, declarations.entrySet()
            .stream()
            .sorted(Comparator.comparingInt(declaration -> hierarchy.indexOf(declaration.getKey())))
            .map(Map.Entry::getValue)
            .toList()));
        return new InterceptorClassModel(element, methods);
    }

    /**
     * The names of a class and of its superclasses, the most general one first, which is the order the
     * specification invokes the interceptor methods of a hierarchy in.
     *
     * @param element The class
     * @return The names, most general superclass first
     */
    private static List<String> hierarchyOf(ClassElement element) {
        List<String> hierarchy = new ArrayList<>();
        for (ClassElement type = element; type != null; type = type.getSuperType().orElse(null)) {
            hierarchy.add(type.getName());
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    /**
     * Reads the binding annotations of an element: the annotations meta-annotated with
     * {@code jakarta.interceptor.InterceptorBinding}.
     *
     * @param element The element
     * @return The binding annotations, in a stable order
     */
    public static List<AnnotationValue<?>> bindingsOf(Element element) {
        AnnotationMetadata annotationMetadata = element.getAnnotationMetadata();
        List<String> names = annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptors.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return List.of();
        }
        // the metadata of a member is read together with the metadata of its class, so a binding of the class
        // would be read as one of the member too; what is wanted of a member is what it declares itself. A class,
        // on the other hand, keeps the bindings it inherits from its superclasses, which is what an @Inherited
        // binding annotation asks for
        boolean isClass = element instanceof ClassElement;
        Set<String> declared = isClass ? Set.of() : Set.copyOf(annotationMetadata.getDeclaredAnnotationNames());
        // a map keyed by name keeps the bindings distinct while preserving the declaration order
        Map<String, AnnotationValue<?>> bindings = new LinkedHashMap<>(names.size());
        for (String name : names) {
            if (JakartaInterceptors.INTERCEPTOR_BINDING.equals(name)) {
                // a binding annotation names itself among its bindings; it is not one of its own
                continue;
            }
            if (!isClass && !declared.contains(name)) {
                continue;
            }
            annotationMetadata.findAnnotation(name).ifPresent(av -> bindings.put(name, av));
        }
        return List.copyOf(bindings.values());
    }

    private static boolean acceptsInvocationContext(MethodElement method) {
        ParameterElement[] parameters = method.getParameters();
        return parameters.length == 1
            && parameters[0].getType().getName().equals(JakartaInterceptors.INVOCATION_CONTEXT);
    }

    private static void validate(ClassElement declaringClass, MethodElement method, String annotation, InterceptionKind kind) {
        if (method.isPrivate()) {
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + declaringClass.getName() + "] is private. An interceptor method is invoked through the "
                + "executable method Micronaut generates for it rather than reflectively, so it has to be at least "
                + "package private");
        }
        if (method.isStatic() || method.isFinal()) {
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + declaringClass.getName() + "] must not be static or final");
        }
        boolean returnsTheResult = kind == InterceptionKind.AROUND_INVOKE || kind == InterceptionKind.AROUND_TIMEOUT;
        if (returnsTheResult && method.getReturnType().isVoid()) {
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + declaringClass.getName() + "] must return Object");
        }
    }

    private static String simpleName(String annotation) {
        return annotation.substring(annotation.lastIndexOf('.') + 1);
    }
}
