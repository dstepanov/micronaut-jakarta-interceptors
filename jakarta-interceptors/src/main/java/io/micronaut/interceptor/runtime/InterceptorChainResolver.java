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

import io.micronaut.aop.Adapter;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.annotation.InterceptionKind;
import io.micronaut.interceptor.annotation.JakartaInterception;
import io.micronaut.interceptor.annotation.JakartaInterceptorIndex;
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
import java.util.Optional;
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
    // an interceptor class describes itself the same way whichever element its chain was resolved for, so what is
    // read of it is read once rather than once for every element it intercepts
    private final Map<Class<?>, Optional<BeanDefinition<?>>> describing = new ConcurrentHashMap<>();
    private final Map<ReferenceKey, List<InterceptorReference>> references = new ConcurrentHashMap<>();

    /**
     * @param beanContext The bean context the interceptor classes are beans of
     */
    public InterceptorChainResolver(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    /**
     * Resolves the chain of an intercepted element.
     *
     * <p>A chain is remembered by what it is built from, which is the interception the processor declared on the
     * element and the kind of interception. Two elements that declare the same interception share one chain, and
     * the same element declared differently by two beans - a method two beans inherit from one superclass, or a
     * class two factory methods produce - has a chain for each declaration.</p>
     *
     * @param interceptorKind The kind Micronaut intercepts the element as
     * @param metadata        The annotation metadata of the element
     * @return The interceptors, in the order they are invoked in
     */
    List<InterceptorReference> resolve(InterceptorKind interceptorKind, AnnotationMetadata metadata) {
        if (metadata.hasAnnotation(Adapter.class)) {
            // Micronaut generates a bean for each adapted method of a class - a method annotated @EventListener,
            // say - which implements the interface the method is adapted to and invokes the method on the bean. It
            // carries the metadata of the class whose method it adapts, so everything that class declares reaches
            // it, the interception included, and it is nothing the application declared: it is the class itself
            // that the specification constructs, initializes and removes, and its methods that are invoked. Leaving
            // the generated bean out is what keeps the interception of that one object to one object's worth -
            // interposing on an invocation of the adapted method here as well would run its chain twice, and
            // interposing on the life of the generated bean would construct a second interceptor instance and hand
            // it an object of a type the binding was never declared on
            return List.of();
        }
        AnnotationValue<JakartaInterception> interception = metadata.getAnnotation(JakartaInterception.class);
        boolean timeout = interception != null && interception.booleanValue("timeout").orElse(false);
        InterceptionKind kind = InterceptionKind.of(interceptorKind, timeout);
        if (kind == null) {
            return List.of();
        }
        ChainKey key = ChainKey.of(kind, interception);
        List<InterceptorReference> chain = chains.get(key);
        if (chain == null) {
            // two threads may build the same chain, and the later put wins. The key is everything the chain is
            // built from, so the two chains are equal and nothing is lost. Building inside a computeIfAbsent would
            // instead hold a lock of the map across the bean context
            chain = build(key);
            chains.put(key, chain);
        }
        return chain;
    }

    /**
     * How many chains have been resolved and remembered.
     *
     * <p>Exposed so that a test can hold this map to its bounds: it is keyed by what a chain is built from rather
     * than by the objects an invocation passes through, and must not grow with the beans of the application.</p>
     *
     * @return The number of chains held
     */
    @Internal
    public int cachedChains() {
        return chains.size();
    }

    private List<InterceptorReference> build(ChainKey key) {
        // the chain is built from the key alone, which is what lets it be remembered under the key
        InterceptionKind kind = key.kind();
        if (key.excluded()) {
            return List.of();
        }
        // a map keyed by the interceptor class keeps the order while making sure an interceptor class that is both
        // named directly and bound by an annotation is only invoked once, at its first position
        Map<Class<?>, BeanDefinition<?>> ordered = new LinkedHashMap<>();
        for (Class<?> interceptorClass : key.interceptors()) {
            ordered.putIfAbsent(interceptorClass, requireDescribing(interceptorClass));
        }
        for (BeanDefinition<?> definition : boundInterceptors(key.bindings())) {
            BeanDefinition<?> describing = describing(definition.getBeanType());
            ordered.putIfAbsent(definition.getBeanType(), describing == null ? definition : describing);
        }
        // the interceptor methods a class declares on itself are invoked after every interceptor class
        Class<?> self = key.self();
        List<InterceptorReference> chain = new ArrayList<>(ordered.size() + 1);
        for (BeanDefinition<?> definition : ordered.values()) {
            chain.addAll(references(definition, kind, false));
        }
        if (self != null) {
            chain.addAll(references(requireDescribing(self), kind, true));
        }
        return List.copyOf(chain);
    }

    /**
     * Finds the interceptor classes whose binding annotations the element declares as well. The specification binds
     * an interceptor to an element when every binding of the interceptor is a binding of the element.
     *
     * <p>What a binding is compared by was worked out by the processor and written out on both of them, so what is
     * compared here are the strings it wrote rather than the annotations themselves.</p>
     */
    private List<BeanDefinition<?>> boundInterceptors(List<String> bindingsOfElement) {
        if (bindingsOfElement.isEmpty()) {
            return List.of();
        }
        Set<String> declared = Set.copyOf(bindingsOfElement);
        List<BeanDefinition<?>> matching = new ArrayList<>();
        for (BeanDefinition<?> definition : allInterceptorClasses()) {
            AnnotationValue<JakartaInterceptorMethods> methods =
                definition.getAnnotation(JakartaInterceptorMethods.class);
            if (methods == null) {
                continue;
            }
            Set<String> bindings = Set.of(methods.stringValues("bindings"));
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
        return describing.computeIfAbsent(interceptorClass, type -> Optional.ofNullable(describe(type)))
            .orElse(null);
    }

    /**
     * The definition that describes an interceptor class the element names, which has to be there.
     *
     * <p>An interceptor class the processor never saw has no recorded interceptor methods, and so nothing the
     * runtime could invoke: either Micronaut generated no definition of it, or the one it generated says nothing of
     * its interceptor methods. Leaving it out of the chain would leave the element intercepted by less than it
     * declares, with nothing to say so, so the chain is not resolved at all. The same goes for the class that
     * declares interceptor methods on itself, which the processor did see, but whose definition may still be
     * missing from the context.</p>
     *
     * <p>An interceptor class bound by a binding annotation cannot be reported this way. It is found among the
     * interceptor classes the processor indexed, so one the processor never saw is not found in the first place.</p>
     *
     * @param interceptorClass The interceptor class
     * @return The definition
     */
    private BeanDefinition<?> requireDescribing(Class<?> interceptorClass) {
        BeanDefinition<?> definition = describing(interceptorClass);
        if (definition == null) {
            throw new IllegalStateException("The interceptor class [" + interceptorClass.getName() + "] has no bean "
                + "definition that describes its interceptor methods, so it cannot intercept anything. An interceptor "
                + "class has to be compiled with micronaut-jakarta-interceptors-processor on the annotation processor "
                + "path, and its bean must not be disabled");
        }
        return definition;
    }

    private @Nullable BeanDefinition<?> describe(Class<?> interceptorClass) {
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
     * <p>They are asked for by the type the processor indexed them under rather than by reading every bean
     * definition of the context and keeping the ones that are interceptor classes. An index is what the context
     * keeps for exactly this question, and it answers it without the size of the application being the cost of
     * resolving one chain.</p>
     *
     * <p>They are looked for again for every chain that is resolved, rather than once and remembered. The first
     * chain of an application is resolved while a bean of it is being created, and the definitions the context
     * knows of at that moment are not yet all of them; a list taken then and kept would leave every interceptor
     * that had not been reached yet out of every chain resolved afterwards. What is remembered is the chain of
     * each intercepted element, so this runs once for each of them rather than once for each invocation.</p>
     */
    private List<BeanDefinition<?>> allInterceptorClasses() {
        List<BeanDefinition<?>> interceptors = new ArrayList<>();
        for (BeanDefinition<?> indexed : beanContext.getBeanDefinitions(JakartaInterceptorIndex.class)) {
            BeanDefinition<?> definition = describing(indexed.getBeanType());
            // a class that declares an interceptor method without declaring itself an interceptor class is one
            // only where it is named directly, and takes no part in what a binding annotation binds
            if (definition != null && definition.hasAnnotation(JakartaInterceptorSupport.INTERCEPTOR)) {
                interceptors.add(definition);
            }
        }
        return interceptors;
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
        AnnotationValue<JakartaInterceptorMethods> methods =
            definition.getAnnotation(JakartaInterceptorMethods.class);
        if (methods == null) {
            return List.of();
        }
        InterceptionKind recorded = kind;
        if (kind == InterceptionKind.AROUND_TIMEOUT && methods.stringValues(kind.member()).length == 0) {
            // the specification has an @AroundInvoke method interpose on business methods alone. An interceptor
            // that declares no @AroundTimeout method would then quietly stop intercepting a method the moment it
            // was scheduled, so its @AroundInvoke methods are used instead
            recorded = InterceptionKind.AROUND_INVOKE;
        }
        String[] names = methods.stringValues(recorded.member());
        AnnotationClassValue<?>[] declaringTypes = methods.annotationClassValues(recorded.declaringTypesMember());
        List<InterceptorReference> references = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; i++) {
            String declaringType = i < declaringTypes.length ? declaringTypes[i].getName() : null;
            ExecutableMethod<Object, Object> method = interceptorMethod(definition, names[i], declaringType);
            references.add(new InterceptorReference(definition.getBeanType(), method, self, isReflective(method)));
        }
        // the list is shared between every chain that includes this interceptor, so it is not one of theirs to
        // change
        return List.copyOf(references);
    }

    /**
     * Whether the executable method of an interceptor method reaches it reflectively rather than calling it.
     *
     * <p>Micronaut reaches a method generated code cannot call - one declared {@code private}, or a {@code protected}
     * or package private one of a superclass in another package - reflectively, once it is told that reflection is
     * permitted, and the processor tells it so for exactly those interceptor methods. The annotation it wrote is
     * therefore what says which of them Micronaut wraps the exception of; see
     * {@link InterceptorReference#invoke(Object, jakarta.interceptor.InvocationContext)}.</p>
     */
    private static boolean isReflective(ExecutableMethod<?, ?> method) {
        return method.getAnnotationMetadata().hasDeclaredAnnotation(ReflectiveAccess.class);
    }

    /**
     * The executable method of one interceptor method.
     *
     * <p>It is found by the class that declares it as well as by its name. The executable methods of a class
     * include the ones it inherits, and a class and its superclass may each declare a private interceptor method of
     * the same name and signature; looked up by name alone, both would be whichever of them came first.</p>
     *
     * @param definition    The definition of the interceptor class
     * @param name          The name of the method
     * @param declaringType The name of the class that declares it, or {@code null} where it was not recorded
     */
    @SuppressWarnings("unchecked")
    private static ExecutableMethod<Object, Object> interceptorMethod(BeanDefinition<?> definition,
                                                                     String name,
                                                                     @Nullable String declaringType) {
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            Class<?>[] argumentTypes = method.getArgumentTypes();
            if (method.getMethodName().equals(name)
                && argumentTypes.length == 1
                && argumentTypes[0] == InvocationContext.class
                && (declaringType == null || method.getDeclaringType().getName().equals(declaringType))) {
                return (ExecutableMethod<Object, Object>) method;
            }
        }
        throw new IllegalStateException("The interceptor method [" + name + "] of ["
            + (declaringType == null ? definition.getBeanType().getName() : declaringType)
            + "] has no executable method. The interceptor class has to be compiled with "
            + "micronaut-jakarta-interceptors-processor on the annotation processor path");
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
     * What a chain is built from, and so what it is remembered by.
     *
     * <p>It is not the element itself. An executable method compares by its declaring type, its name and its
     * argument types, so a method two beans inherit from one superclass is one key for both, and so is a class
     * two factory methods produce, even where each of them binds it to different interceptors; the class of the
     * intercepted object tells neither apart. What does tell them apart is the interception the processor declared
     * on the element: the interceptor classes the element names, its bindings, its own interceptor methods and
     * whether it excludes the rest, which with the kind is all a chain is built from. Equal keys therefore build
     * equal chains, and the map stays as large as the number of different interceptions an application declares
     * rather than growing with its elements or its beans.</p>
     *
     * <p>Those members are read out into this key rather than the annotation itself being held, for two reasons.
     * {@code AnnotationValue.equals} compares the sizes of the two member maps and then skips a member the other
     * map does not have, so an element declaring {@code self} and an element declaring {@code bindings} compare
     * equal - and, for the right class name, hash alike - while naming entirely different interceptor methods; one
     * of them would then be intercepted by the chain of the other. And an annotation read from metadata that
     * contains an evaluated expression is handed back wrapped around the evaluation context of the invocation,
     * which holds the object being invoked and its arguments; holding that as a key would keep them alive for as
     * long as this resolver. What is held here is what the processor wrote: class objects and strings, which belong
     * to no invocation.</p>
     *
     * @param kind         The kind of interception
     * @param interceptors The interceptor classes the element names, in the order they are invoked in
     * @param bindings     What the binding annotations of the element are compared by
     * @param self         The class whose own interceptor methods interpose on the element, or {@code null}
     * @param excluded     Whether the element is not intercepted at all
     */
    record ChainKey(InterceptionKind kind,
                    List<Class<?>> interceptors,
                    List<String> bindings,
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
                return new ChainKey(kind, List.of(), List.of(), null, false);
            }
            return new ChainKey(kind,
                List.of(interception.classValues("interceptors")),
                List.of(interception.stringValues("bindings")),
                interception.classValue("self").filter(type -> type != void.class).orElse(null),
                interception.booleanValue("excluded").orElse(false));
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
