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
package io.micronaut.interceptor.runtime;

import io.micronaut.aop.InterceptorKind;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.annotation.JakartaInterception;
import io.micronaut.interceptor.annotation.JakartaInterceptorMethods;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves, and remembers, the chain of interceptor classes that applies to an intercepted element.
 *
 * <p>The interceptor classes an element names with {@code jakarta.interceptor.Interceptors} were already resolved
 * and ordered at compilation time and are read back from the annotation the processor left on the element. The
 * interceptor classes bound by a binding annotation are looked for among the beans, because an interceptor class
 * and the elements it intercepts need not be compiled together, and are ordered by their priority. The
 * specification invokes the ones named directly first, which is the order the two lists are joined in.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Singleton
@Internal
public final class InterceptorChainResolver {

    private final BeanContext beanContext;
    private final Map<ChainKey, List<InterceptorReference>> chains = new ConcurrentHashMap<>();

    /**
     * @param beanContext The bean context the interceptor classes are beans of
     */
    public InterceptorChainResolver(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * Resolves the chain of an intercepted element.
     *
     * @param key      What identifies the element
     * @param metadata The annotation metadata of the element
     * @return The interceptors, in the order they are invoked in
     */
    List<InterceptorReference> resolve(ChainKey key, AnnotationMetadata metadata) {
        List<InterceptorReference> chain = chains.get(key);
        if (chain == null) {
            // two threads may build the same chain, and the later put wins; the chains are equal, so nothing is
            // lost. Building inside a computeIfAbsent would instead hold a lock of the map across the bean context
            chain = build(metadata, key.kind());
            chains.put(key, chain);
        }
        return chain;
    }

    /**
     * How many chains have been resolved and remembered.
     *
     * <p>Exposed so that a test can hold this map to its bounds: it is keyed by what identifies an intercepted
     * element rather than by the objects an invocation passes through, and must not grow with the beans of the
     * application.</p>
     *
     * @return The number of chains held
     */
    @Internal
    public int cachedChains() {
        return chains.size();
    }

    private List<InterceptorReference> build(AnnotationMetadata metadata, InterceptorKind interceptorKind) {
        AnnotationValue<JakartaInterception> interception = metadata.getAnnotation(JakartaInterception.class);
        if (interception != null && interception.booleanValue("excluded").orElse(false)) {
            return List.of();
        }
        boolean timeout = interception != null && interception.booleanValue("timeout").orElse(false);
        InterceptionKind kind = InterceptionKind.of(interceptorKind, timeout);
        if (kind == null) {
            return List.of();
        }
        // a map keyed by the interceptor class keeps the order while making sure an interceptor class that is both
        // named directly and bound by an annotation is only invoked once, at its first position
        Map<Class<?>, BeanDefinition<?>> ordered = new LinkedHashMap<>();
        for (Class<?> interceptorClass : namedInterceptors(interception)) {
            BeanDefinition<?> definition = describing(interceptorClass);
            if (definition != null) {
                ordered.putIfAbsent(interceptorClass, definition);
            }
        }
        for (BeanDefinition<?> definition : boundInterceptors(metadata)) {
            BeanDefinition<?> describing = describing(definition.getBeanType());
            ordered.putIfAbsent(definition.getBeanType(), describing == null ? definition : describing);
        }
        // the interceptor methods a class declares on itself are invoked after every interceptor class
        Class<?> self = interception == null ? null
            : interception.classValue("self").filter(type -> type != void.class).orElse(null);
        List<InterceptorReference> chain = new ArrayList<>(ordered.size() + 1);
        for (BeanDefinition<?> definition : ordered.values()) {
            chain.addAll(references(definition, kind, false));
        }
        if (self != null) {
            BeanDefinition<?> definition = describing(self);
            if (definition != null) {
                chain.addAll(references(definition, kind, true));
            }
        }
        return List.copyOf(chain);
    }

    /**
     * Reads the interceptor classes the processor resolved for the element at compilation time.
     */
    private static List<Class<?>> namedInterceptors(@Nullable AnnotationValue<JakartaInterception> interception) {
        if (interception == null) {
            return List.of();
        }
        return List.of(interception.classValues("interceptors"));
    }

    /**
     * Finds the interceptor classes whose binding annotations the element declares as well. The specification binds
     * an interceptor to an element when every binding of the interceptor is a binding of the element.
     */
    private List<BeanDefinition<?>> boundInterceptors(AnnotationMetadata metadata) {
        Set<InterceptorBindingValues.Binding> declared = InterceptorBindingValues.of(metadata);
        if (declared.isEmpty()) {
            return List.of();
        }
        List<BeanDefinition<?>> matching = new ArrayList<>();
        for (BeanDefinition<?> definition : allInterceptorClasses()) {
            Set<InterceptorBindingValues.Binding> bindings =
                InterceptorBindingValues.of(definition.getAnnotationMetadata());
            if (!bindings.isEmpty() && declared.containsAll(bindings)) {
                matching.add(definition);
            }
        }
        matching.sort(Comparator.comparingInt(InterceptorChainResolver::priorityOf)
            .thenComparing(definition -> definition.getBeanType().getName()));
        return matching;
    }

    /**
     * The definition of an interceptor class that describes its interceptor methods.
     *
     * <p>There may be more than one definition of a class: this module makes an interceptor class a bean of its
     * own, and an application may produce the same class from a factory to configure it. Only the definition
     * Micronaut generated from the class carries the executable methods the interceptor methods are invoked
     * through, and it is the one read here; which of them provides the instance is a separate question, answered
     * by the bean context.</p>
     */
    private @Nullable BeanDefinition<?> describing(Class<?> interceptorClass) {
        BeanDefinition<?> fallback = null;
        for (BeanDefinition<?> definition : beanContext.getBeanDefinitions(interceptorClass)) {
            if (definition.getAnnotation(JakartaInterceptorMethods.class) == null) {
                continue;
            }
            if (!definition.getExecutableMethods().isEmpty()) {
                return definition;
            }
            fallback = definition;
        }
        return fallback;
    }

    /**
     * All the interceptor classes of the context.
     *
     * <p>They are looked for again for every chain that is resolved, rather than once and remembered. The first
     * chain of an application is resolved while a bean of it is being created, and the definitions the context
     * knows of at that moment are not yet all of them; a list taken then and kept would leave every interceptor
     * that had not been reached yet out of every chain resolved afterwards. What is remembered is the chain of
     * each intercepted element, so this runs once for each of them rather than once for each invocation.</p>
     */
    private List<BeanDefinition<?>> allInterceptorClasses() {
        return beanContext.getAllBeanDefinitions()
            .stream()
            .filter(definition -> definition.hasAnnotation(JakartaInterceptorSupport.INTERCEPTOR))
            .<BeanDefinition<?>>map(definition -> definition)
            .toList();
    }

    /**
     * The interceptor methods of one interceptor class that interpose on a kind of interception, in the order the
     * specification invokes them: the ones its superclasses declare first, its own last.
     */
    @SuppressWarnings("unchecked")
    private static List<InterceptorReference> references(BeanDefinition<?> definition, InterceptionKind kind, boolean self) {
        AnnotationValue<JakartaInterceptorMethods> methods =
            definition.getAnnotation(JakartaInterceptorMethods.class);
        if (methods == null) {
            return List.of();
        }
        String[] names = methods.stringValues(kind.member());
        if (names.length == 0 && kind == InterceptionKind.AROUND_TIMEOUT) {
            // the specification has an @AroundInvoke method interpose on business methods alone. An interceptor
            // that declares no @AroundTimeout method would then quietly stop intercepting a method the moment it
            // was scheduled, so its @AroundInvoke methods are used instead
            names = methods.stringValues(InterceptionKind.AROUND_INVOKE.member());
        }
        List<InterceptorReference> references = new ArrayList<>(names.length);
        for (String name : names) {
            ExecutableMethod<Object, Object> method = (ExecutableMethod<Object, Object>) definition
                .findMethod(name, InvocationContext.class)
                .orElseThrow(() -> new IllegalStateException("The interceptor method [" + name + "] of ["
                    + definition.getBeanType().getName() + "] has no executable method. The interceptor class has "
                    + "to be compiled with the Jakarta Interceptors annotation processor"));
            references.add(new InterceptorReference(definition.getBeanType(), method, self));
        }
        return references;
    }

    /**
     * The priority an interceptor class is ordered by, which the specification takes from
     * {@code jakarta.annotation.Priority}. Micronaut maps that annotation onto its own {@code @Order}, which is
     * read as a fallback so that an interceptor ordered the Micronaut way is ordered the same.
     */
    private static int priorityOf(BeanDefinition<?> definition) {
        AnnotationMetadata metadata = definition.getAnnotationMetadata();
        OptionalInt priority = metadata.intValue(JakartaInterceptorSupport.PRIORITY, AnnotationMetadata.VALUE_MEMBER);
        if (priority.isPresent()) {
            return priority.getAsInt();
        }
        return metadata.intValue(Order.class).orElse(Interceptor.Priority.APPLICATION);
    }

    /**
     * Identifies an intercepted element, so that the chain resolved for it is resolved once.
     *
     * <p>What identifies it depends on the kind. A business or timeout method is identified by its executable
     * method, which compares by its declaring type, its name and its argument types: overloads of one name are
     * different elements and must not share a chain. A lifecycle callback and a constructor are identified by the
     * class instead, because the executable method of a callback is created anew for every bean and would make
     * this map grow with them, and because a bean has one chain for each of them either way.</p>
     *
     * @param element What the chain was resolved for
     * @param kind    The kind of interception
     */
    record ChainKey(Object element, InterceptorKind kind) {
    }
}
