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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Map<InterceptionKind, List<MethodElement>> methods = new HashMap<>();
        // a class declares at most one interceptor method of a kind, but its superclasses declare their own, and
        // the specification invokes all of them with the most general superclass first
        List<String> hierarchy = hierarchyOf(element);
        Map<InterceptionKind, Map<String, MethodElement>> byDeclaringClass = new HashMap<>();
        // a declaration that does not accept an InvocationContext is not passed over: it is reported, once it is
        // known who declared it. Which of them are reported depends on what the class turned out to be, so they are
        // collected while the class is read and reported after
        Map<MethodElement, String> malformed = new LinkedHashMap<>();
        for (MethodElement method : element.getEnclosedElements(ElementQuery.ALL_METHODS)) {
            for (Map.Entry<String, InterceptionKind> entry : INTERCEPTOR_METHODS.entrySet()) {
                if (!method.hasDeclaredAnnotation(entry.getKey())) {
                    continue;
                }
                if (!acceptsInvocationContext(method)) {
                    if (!isOwnLifecycleCallback(method, entry.getValue())) {
                        malformed.put(method, entry.getKey());
                    }
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
        reportMalformed(element, byDeclaringClass.keySet(), malformed);
        byDeclaringClass.forEach((kind, declarations) -> methods.put(kind, declarations.entrySet()
            .stream()
            .sorted(Comparator.comparingInt(declaration -> hierarchy.indexOf(declaration.getKey())))
            .map(Map.Entry::getValue)
            .toList()));
        return new InterceptorClassModel(element, methods);
    }

    /**
     * Tells whether a lifecycle annotation on a method that accepts no {@code InvocationContext} is the class
     * declaring a callback of its own lifecycle, which any class may do - an interceptor class included.
     *
     * <p>Such a callback takes no argument at all. One declared by an interceptor class is invoked when the
     * interceptor itself is created or destroyed, rather than when the objects it intercepts are, and the
     * specification says as much of it in section 2.7.</p>
     */
    private static boolean isOwnLifecycleCallback(MethodElement method, InterceptionKind kind) {
        boolean lifecycle = kind == InterceptionKind.POST_CONSTRUCT || kind == InterceptionKind.PRE_DESTROY;
        return lifecycle && method.getParameters().length == 0;
    }

    /**
     * Reports a declaration the specification gives a signature that this one does not have.
     *
     * <p>Sections 2.6 and 2.7 give every interceptor method one parameter, an {@code InvocationContext}. A
     * declaration with any other parameters interposes on nothing, and a class that declares one beside a valid
     * interceptor method would otherwise satisfy every check there is and have that declaration left out of its
     * chains without a word. The reference implementation refuses to deploy such a class, and this refuses to
     * compile it.</p>
     *
     * <p>A lifecycle annotation is the one that says something else as well. Micronaut invokes a
     * {@code @PostConstruct} or {@code @PreDestroy} method of a bean with whatever it asks to have injected into it,
     * which is a callback of the class rather than a malformed interceptor method, so one is reported only where the
     * class it belongs to is an interceptor class: where it declares {@code @Interceptor}, or where it interposes on
     * the construction or the lifecycle of another object and its lifecycle callbacks are therefore read as
     * interposing too.</p>
     *
     * @param element    The class
     * @param interposes The kinds of interception the class was found to interpose on
     * @param malformed  The declarations that accept no {@code InvocationContext}, by the annotation naming each
     */
    private static void reportMalformed(ClassElement element,
                                        Set<InterceptionKind> interposes,
                                        Map<MethodElement, String> malformed) {
        if (malformed.isEmpty()) {
            return;
        }
        boolean interceptorClass = element.hasDeclaredAnnotation(JakartaInterceptors.INTERCEPTOR)
            || interposes.stream().anyMatch(kind -> kind != InterceptionKind.AROUND_INVOKE
                && kind != InterceptionKind.AROUND_TIMEOUT);
        for (Map.Entry<MethodElement, String> entry : malformed.entrySet()) {
            MethodElement method = entry.getKey();
            String annotation = entry.getValue();
            InterceptionKind kind = INTERCEPTOR_METHODS.get(annotation);
            boolean lifecycle = kind == InterceptionKind.POST_CONSTRUCT || kind == InterceptionKind.PRE_DESTROY;
            if (lifecycle && !interceptorClass) {
                continue;
            }
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + method.getDeclaringType().getName() + "] must accept a single "
                + JakartaInterceptors.INVOCATION_CONTEXT + (lifecycle ? ", or no parameter at all, which is how a "
                + "class declares a callback of its own lifecycle" : "") + ", but it accepts ["
                + parameterTypesOf(method) + "]");
        }
    }

    private static String parameterTypesOf(MethodElement method) {
        return Arrays.stream(method.getParameters())
            .map(parameter -> parameter.getType().getName())
            .collect(Collectors.joining(", "));
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
     * {@code jakarta.interceptor.InterceptorBinding}, and the binding annotations those carry.
     *
     * <p>A binding annotation declared on another annotation binds through it whatever that annotation is
     * otherwise for, so a method declaring an annotation that carries {@code @Logged} is bound by {@code @Logged}
     * as much as a method declaring {@code @Logged} is.</p>
     *
     * @param element The element
     * @return The binding annotations, in a stable order
     */
    public static List<AnnotationValue<?>> bindingsOf(Element element) {
        AnnotationMetadata annotationMetadata = ownMetadataOf(element);
        List<String> names = annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptors.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return List.of();
        }
        // what is wanted of a member is what it declares itself, which its own metadata holds apart from its class,
        // and what the annotations it declares carry. Micronaut holds the second apart from the first, as the
        // stereotypes of the member, so both are read; an annotation it only inherits from a method it overrides is
        // neither. A class, on the other hand, keeps the bindings it inherits from its superclasses, which is what
        // an @Inherited binding annotation asks for
        boolean isClass = element instanceof ClassElement;
        Set<String> declared = new HashSet<>();
        if (!isClass) {
            declared.addAll(annotationMetadata.getDeclaredAnnotationNames());
            declared.addAll(annotationMetadata.getDeclaredStereotypeAnnotationNames());
        }
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

    /**
     * The metadata of an element without the metadata of the class it belongs to.
     *
     * <p>The metadata Micronaut hands out for a method is read together with the metadata of its class, and it
     * merges the two member by member: a member the method leaves to its default is answered with the value the
     * class gives it. That is not what a binding the method declares means. It replaces the whole binding of its
     * class, so {@code @Zone} on a method of a class declaring {@code @Zone("a")} is bound by the default of
     * {@code value}, not by {@code "a"}. The metadata of the method alone is what says that.</p>
     *
     * @param element The element
     * @return The metadata of a method or a constructor alone, or the metadata of any other element
     */
    public static AnnotationMetadata ownMetadataOf(Element element) {
        if (element instanceof MethodElement method) {
            return method.getMethodAnnotationMetadata();
        }
        return element.getAnnotationMetadata();
    }

    private static boolean acceptsInvocationContext(MethodElement method) {
        ParameterElement[] parameters = method.getParameters();
        return parameters.length == 1
            && parameters[0].getType().getName().equals(JakartaInterceptors.INVOCATION_CONTEXT);
    }

    /**
     * Checks an interceptor method against the signature the specification gives it.
     *
     * <p>A method that interposes on a business or a timeout method returns what it interposed on, so it returns
     * {@code Object}. One that interposes on the construction of an object or on a lifecycle callback has nothing
     * to return - what it does return is discarded - so it returns either {@code void} or {@code Object}, the
     * second of which is what lets one method interpose on both.</p>
     *
     * <p>None of them can be static, final or abstract: an interceptor method is invoked on an instance of the
     * class that declares it, and it is the declaration itself that is invoked rather than an override of it.</p>
     */
    private static void validate(ClassElement declaringClass, MethodElement method, String annotation, InterceptionKind kind) {
        if (method.isStatic() || method.isFinal() || method.isAbstract()) {
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + declaringClass.getName() + "] must not be static, final or abstract");
        }
        boolean returnsTheResult = kind == InterceptionKind.AROUND_INVOKE || kind == InterceptionKind.AROUND_TIMEOUT;
        boolean returnsObject = Object.class.getName().equals(method.getReturnType().getName());
        if (returnsTheResult && !returnsObject) {
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + declaringClass.getName() + "] must return Object, but it returns ["
                + method.getReturnType().getName() + "]");
        }
        if (!returnsTheResult && !returnsObject && !method.getReturnType().isVoid()) {
            throw new ProcessingException(method, "The @" + simpleName(annotation) + " method [" + method.getName()
                + "] of [" + declaringClass.getName() + "] must return void or Object, but it returns ["
                + method.getReturnType().getName() + "]");
        }
    }

    private static String simpleName(String annotation) {
        return annotation.substring(annotation.lastIndexOf('.') + 1);
    }
}
