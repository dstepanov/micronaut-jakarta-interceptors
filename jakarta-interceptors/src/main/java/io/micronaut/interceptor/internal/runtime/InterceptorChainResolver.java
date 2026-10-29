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
package io.micronaut.interceptor.internal.runtime;

import io.micronaut.interceptor.spi.BoundInterceptorEnablement;
import io.micronaut.interceptor.spi.InterceptorBindings;
import io.micronaut.interceptor.spi.InterceptorMethods;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.internal.metadata.JakartaInterception;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

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

    // the chains, under the interception as the processor recorded it, so that finding the chain of an element a
    // second time reads nothing into a ChainKey
    private final Map<RecordedKey, List<InterceptorReference>> recorded = new ConcurrentHashMap<>();
    private final LongAdder readInterceptions = new LongAdder();
    private final Map<ReferenceKey, List<InterceptorReference>> references = new ConcurrentHashMap<>();
    // the interceptor classes whose instances are created with an object, by the definition of the object, which is
    // equal for every bean of it a definition Micronaut compiled creates
    private final Map<BeanDefinition<?>, List<Class<?>>> interceptorClasses = new ConcurrentHashMap<>();
    // which interceptor classes there are to intercept with: the definitions describing them, and which of them a
    // binding binds
    private final InterceptorDefinitions definitions;

    /**
     * @param beanContext The bean context the interceptor classes are beans of
     * @param enablements What decides whether an interceptor class a binding binds is enabled, none unless a module
     *                    with rules of enablement provides one
     */
    public InterceptorChainResolver(BeanContext beanContext, List<BoundInterceptorEnablement> enablements) {
        this.definitions = new InterceptorDefinitions(beanContext, enablements);
    }

    /**
     * Resolves the chain of an intercepted element.
     *
     * <p>A chain is remembered under the interception exactly as the processor recorded it on the element, and the
     * kind of interception, see {@link RecordedKey}. Two elements recorded alike share one chain, and the same
     * element declared differently by two beans - a method two beans inherit from one superclass, or a class two
     * factory methods produce - has a chain for each declaration. The bindings of an element are brought into the
     * shape they are compared in once, when its chain is first resolved, rather than every time a bean is created
     * or an invocation looks it up.</p>
     *
     * @param interceptorKind The kind Micronaut intercepts the element as
     * @param metadata        The annotation metadata of the element
     * @return The interceptors, in the order they are invoked in
     */
    List<InterceptorReference> resolve(InterceptorKind interceptorKind, AnnotationMetadata metadata) {
        return resolve(interceptorKind, metadata, true);
    }

    /**
     * Resolves the chain of an intercepted element, telling whether the scheduler invoked it.
     *
     * @param interceptorKind The kind Micronaut intercepts the element as
     * @param metadata        The annotation metadata of the element
     * @param scheduled       Whether the scheduler is invoking the element, without which a timeout method is
     *                        interposed on as a business method
     * @return The interceptors, in the order they are invoked in
     */
    List<InterceptorReference> resolve(InterceptorKind interceptorKind, AnnotationMetadata metadata, boolean scheduled) {
        AnnotationValue<JakartaInterception> interception = recordedInterception(metadata);
        boolean timeout = scheduled && interception != null && interception.booleanValue(JakartaInterception.TIMEOUT).orElse(false);
        InterceptionKind kind = InterceptionKind.of(interceptorKind, timeout);
        if (kind == null) {
            return List.of();
        }
        return resolve(RecordedKey.of(kind, interception), kind, interception);
    }

    /**
     * Resolves the chain of a method, invoked as a business method or running a lifecycle event of its bean.
     *
     * <p>The interception is read from what the method keeps of it, see {@link MethodInterception}: the method's
     * metadata is the metadata of every invocation of it, beneath the wrapper an evaluated expression puts around
     * it, and what it records is read once for the method rather than once for every bean created that invokes
     * it.</p>
     *
     * @param interceptorKind The kind Micronaut intercepts the method as
     * @param method          The method
     * @param scheduled       Whether the scheduler is invoking the method, without which a timeout method is
     *                        interposed on as a business method
     * @return The interceptors, in the order they are invoked in
     */
    List<InterceptorReference> resolve(InterceptorKind interceptorKind, ExecutableMethod<?, ?> method, boolean scheduled) {
        MethodInterception recorded = interceptionOf(method);
        InterceptionKind kind = InterceptionKind.of(interceptorKind, scheduled && recorded.timeout());
        if (kind == null) {
            return List.of();
        }
        return resolve(recorded.key(kind), kind, recorded.interception());
    }

    /**
     * Whether a method is a timeout method when the scheduler invokes it.
     *
     * @param method The method
     * @return Whether it is
     */
    boolean isTimeout(ExecutableMethod<?, ?> method) {
        return interceptionOf(method).timeout();
    }

    private static MethodInterception interceptionOf(ExecutableMethod<?, ?> method) {
        // the interception holds no evaluated expression and is read beneath the wrapper one puts around the
        // metadata, so it is kept for a method whose other annotations have one as well, which getCachedValue would
        // compute again on every call
        return method.getCachedUnevaluatedValue(MethodInterception.class, MethodInterception::of);
    }

    private List<InterceptorReference> resolve(RecordedKey recordedKey,
                                               InterceptionKind kind,
                                               @Nullable AnnotationValue<JakartaInterception> interception) {
        List<InterceptorReference> chain = recorded.get(recordedKey);
        if (chain == null) {
            readInterceptions.increment();
            // two threads may build the same chain, and the later put wins. The key is everything the chain is
            // built from, so the two chains are equal and nothing is lost. Building inside a computeIfAbsent would
            // instead hold a lock of the map across the bean context
            chain = build(ChainKey.of(kind, interception));
            recorded.put(recordedKey, chain);
        }
        return chain;
    }

    /**
     * The interception the processor recorded on an element, read from the metadata Micronaut compiled.
     *
     * <p>Micronaut hands the advice the metadata of an element whose annotations contain an evaluated expression
     * wrapped around the evaluation context of the invocation, and an annotation read through that wrapper holds the
     * invocation. The interception holds no expression, so it reads the same beneath the wrapper, and what is read
     * there can be remembered without keeping an invocation alive.</p>
     */
    static @Nullable AnnotationValue<JakartaInterception> recordedInterception(AnnotationMetadata metadata) {
        return metadata.getTargetAnnotationMetadata().getAnnotation(JakartaInterception.class);
    }

    /**
     * Resolves the interceptor classes associated with an object the definition creates, whose instances are
     * created with the object: those of its construction, read from the constructor, those the class names or binds,
     * read from the class, and those of each of its methods.
     *
     * <p>Those of the construction come first, as the ones the construction of the object runs through: Micronaut
     * creates the advice of a subclass it generates as it resolves the arguments of the constructor, before the
     * around-construct chain starts, and the instances are created in this order then. Section 2.3 da) has every
     * interceptor class associated with the class created before an {@code @AroundConstruct} method runs, which is
     * more than the chains that run: a class the constructor excludes, or replaces the binding of with one of its
     * own, and a class with no interceptor method of a kind anything of the object is intercepted as, such as an
     * {@code @AroundTimeout} method of a bean nothing schedules. The interception of the class is therefore resolved
     * for every kind, and the class-level classes of each kind are among those created.</p>
     *
     * <p>Only what this module intercepts is asked for: an element it does not intercept has no chain, and resolving
     * one would put an empty chain in the map for nothing. An interceptor method the class declares itself runs on
     * the object, and has no instance of its own. The classes are remembered by the definition, rather than read from
     * every chain of the object again for every bean of it created; two threads resolving them for one definition
     * resolve the same.</p>
     *
     * @param definition The definition of the object
     * @return The interceptor classes, each once, in the order they are first met
     */
    List<Class<?>> resolveInterceptorClasses(BeanDefinition<?> definition) {
        List<Class<?>> resolved = interceptorClasses.get(definition);
        if (resolved == null) {
            Set<Class<?>> classes = new LinkedHashSet<>();
            AnnotationMetadata constructorMetadata = definition.getConstructor().getAnnotationMetadata();
            if (constructorMetadata.hasAnnotation(JakartaInterception.class)) {
                addInterceptorClasses(resolve(InterceptorKind.AROUND_CONSTRUCT, constructorMetadata), classes);
            }
            AnnotationValue<JakartaInterception> classInterception =
                recordedInterception(definition.getAnnotationMetadata());
            if (classInterception != null) {
                for (InterceptionKind kind : InterceptionKind.values()) {
                    addInterceptorClasses(resolve(RecordedKey.of(kind, classInterception), kind, classInterception), classes);
                }
            }
            for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
                if (method.getAnnotationMetadata().hasAnnotation(JakartaInterception.class)) {
                    addInterceptorClasses(resolve(InterceptorKind.AROUND, method, true), classes);
                }
            }
            resolved = List.copyOf(classes);
            interceptorClasses.put(definition, resolved);
        }
        return resolved;
    }

    private static void addInterceptorClasses(List<InterceptorReference> chain, Set<Class<?>> classes) {
        for (InterceptorReference reference : chain) {
            if (!reference.self()) {
                classes.add(reference.interceptorClass());
            }
        }
    }

    /**
     * How many chains have been resolved and remembered.
     *
     * <p>Exposed so that a test can hold this map to its bounds: it is keyed by the interception the processor
     * recorded rather than by the objects an invocation passes through, and must not grow with the beans of the
     * application.</p>
     *
     * @return The number of chains held
     */
    @Internal
    public int cachedChains() {
        return recorded.size();
    }

    /**
     * How many times an interception the processor recorded has been read into what a chain is built from.
     *
     * <p>Exposed so that a test can hold the resolver to reading the interception of an element once, rather than
     * again for every bean created and every invocation that looks its chain up.</p>
     *
     * @return The number of interceptions read
     */
    @Internal
    public long readInterceptions() {
        return readInterceptions.sum();
    }

    private List<InterceptorReference> build(ChainKey key) {
        // the chain is built from the key alone, which is what lets it be remembered under the key
        InterceptionKind kind = key.kind();
        if (key.excluded()) {
            return List.of();
        }
        Collection<BeanDefinition<?>> ordered = ordered(key.interceptors(), key.bindings());
        // the interceptor methods a class declares on itself are invoked after every interceptor class
        Class<?> self = key.self();
        List<InterceptorReference> chain = new ArrayList<>(ordered.size() + 1);
        for (BeanDefinition<?> definition : ordered) {
            chain.addAll(references(definition, kind, false));
        }
        if (self != null) {
            chain.addAll(references(definitions.requireDescribing(self), kind, true));
        }
        return List.copyOf(chain);
    }

    /**
     * Finds the interceptor classes whose binding annotations the element declares as well, see
     * {@link InterceptorDefinitions#boundInterceptors(InterceptorBindings)}.
     *
     * <p>It is the selection {@link InterceptorBindings#boundInterceptors(BeanContext, InterceptorBindings)} makes
     * for another module, which asks it of this resolver so that the two cannot differ.</p>
     *
     * @param bindingsOfElement The bindings of the interception point
     * @return The definitions that describe the interceptor classes bound to the point, in the order the chain
     * invokes them in
     */
    @Internal
    public List<BeanDefinition<?>> boundInterceptors(InterceptorBindings bindingsOfElement) {
        return definitions.boundInterceptors(bindingsOfElement);
    }

    /**
     * The definitions of the interceptor classes named directly and of those the bindings bind, in the order their
     * interceptor methods are invoked in.
     *
     * <p>A map keyed by the interceptor class keeps the order while making sure an interceptor class that is both
     * named directly and bound by an annotation is only invoked once, at its first position.</p>
     */
    private Collection<BeanDefinition<?>> ordered(List<Class<?>> interceptors, InterceptorBindings bindings) {
        Map<Class<?>, BeanDefinition<?>> ordered = new LinkedHashMap<>();
        for (Class<?> interceptorClass : interceptors) {
            ordered.putIfAbsent(interceptorClass, definitions.requireDescribing(interceptorClass));
        }
        for (BeanDefinition<?> definition : definitions.boundInterceptors(bindings)) {
            BeanDefinition<?> describing = definitions.describing(definition.getBeanType());
            ordered.putIfAbsent(definition.getBeanType(), describing == null ? definition : describing);
        }
        return ordered.values();
    }

    /**
     * The interceptor methods of one interceptor class that interpose on a kind of interception, in the order the
     * specification invokes them: the ones its superclasses declare first, its own last.
     */
    private List<InterceptorReference> references(BeanDefinition<?> definition, InterceptionKind kind, boolean self) {
        // the interceptor methods of a class that interpose on a kind are the same for every element whose chain
        // includes it, so the list is built once and shared between them
        return references.computeIfAbsent(
            new ReferenceKey(definition.getBeanType(), kind, self),
            key -> referencesOf(definition, kind, self));
    }

    private static List<InterceptorReference> referencesOf(BeanDefinition<?> definition, InterceptionKind kind, boolean self) {
        InterceptorMethods declared = InterceptorMethods.of(definition, kind);
        if (kind == InterceptionKind.AROUND_TIMEOUT && declared.isEmpty()) {
            // the specification has an @AroundInvoke method interpose on business methods alone. An interceptor
            // that declares no @AroundTimeout method would then quietly stop intercepting a method the moment it
            // was scheduled, so its @AroundInvoke methods are used instead
            declared = InterceptorMethods.of(definition, InterceptionKind.AROUND_INVOKE);
        }
        List<ExecutableMethod<Object, Object>> methods = declared.methods();
        List<InterceptorReference> references = new ArrayList<>(methods.size());
        for (ExecutableMethod<Object, Object> method : methods) {
            references.add(new InterceptorReference(definition.getBeanType(), method, self));
        }
        // the list is shared between every chain that includes this interceptor, so it is not one of theirs to
        // change
        return List.copyOf(references);
    }

    /**
     * What a chain is built from: the interception the processor declared on the element, read into the shape a
     * chain is built in, and the kind of interception. It is built once for each chain, when the chain is first
     * resolved, and is not held; the chain is remembered under the {@link RecordedKey} of the element.
     *
     * @param kind         The kind of interception
     * @param interceptors The interceptor classes the element names, in the order they are invoked in
     * @param bindings     The bindings of the element
     * @param self         The class whose own interceptor methods interpose on the element, or {@code null}
     * @param excluded     Whether the element is not intercepted at all
     */
    record ChainKey(InterceptionKind kind,
                    List<Class<?>> interceptors,
                    InterceptorBindings bindings,
                    @Nullable Class<?> self,
                    boolean excluded) {

        /**
         * Reads the key of an element out of the interception the processor declared on it.
         *
         * @param kind         The kind of interception
         * @param interception The interception, or {@code null} where the element declares none
         * @return The key
         */
        static ChainKey of(InterceptionKind kind, @Nullable AnnotationValue<JakartaInterception> interception) {
            if (interception == null) {
                return new ChainKey(kind, List.of(), InterceptorBindings.ofValues(List.of()), null, false);
            }
            return new ChainKey(kind,
                List.of(interception.classValues(JakartaInterception.INTERCEPTORS)),
                InterceptorBindings.ofRecorded(interception.getAnnotations(JakartaInterception.BINDINGS)),
                interception.classValue(JakartaInterception.SELF).filter(type -> type != void.class).orElse(null),
                interception.booleanValue(JakartaInterception.EXCLUDED).orElse(false));
        }
    }

    /**
     * Identifies the interceptor methods of one interceptor class that interpose on one kind of interception,
     * invoked on an interceptor of its own or on the intercepted instance.
     *
     * @param interceptorClass The interceptor class
     * @param kind             The kind of interception
     * @param self             Whether the methods are invoked on the intercepted instance
     */
    private record ReferenceKey(Class<?> interceptorClass, InterceptionKind kind, boolean self) {
    }
}
