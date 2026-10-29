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

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.AdvisedBeanType;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanType;
import io.micronaut.inject.ProxyBeanDefinition;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorIndex;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorMethods;
import io.micronaut.interceptor.internal.metadata.JakartaInterceptorSupport;
import io.micronaut.interceptor.metadata.InterceptionKind;
import io.micronaut.interceptor.spi.BoundInterceptorEnablement;
import io.micronaut.interceptor.spi.InterceptorBindings;
import io.micronaut.interceptor.spi.InterceptorMethods;
import jakarta.interceptor.InvocationContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects the definitions of the interceptor classes a chain is built from: the definition that describes the
 * interceptor methods of a class, and the interceptor classes a binding binds, enabled and in order.
 *
 * <p>Which bean supplies the instance of an interceptor class is a separate question, answered by the bean context
 * when the chain is invoked; this is the part of {@link InterceptorChainResolver} that reads the definitions. It is
 * created by the resolver and lives as long as it does.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterceptorDefinitions {

    private final BeanContext beanContext;
    // an interceptor class describes itself the same way whichever element its chain was resolved for, so what is
    // read of it is read once rather than once for every element it intercepts
    private final Map<Class<?>, Optional<BeanDefinition<?>>> describing = new ConcurrentHashMap<>();
    // the bindings an interceptor class declares, by its indexed definition, which every chain resolved asks of it
    private final Map<BeanType<?>, InterceptorBindings> interceptorBindings = new ConcurrentHashMap<>();
    // what decides whether an interceptor class a binding binds is enabled: none on its own. Injected rather than
    // looked up on the first chain, so that the resolver holds no state it fills in after it is created
    private final List<BoundInterceptorEnablement> enablements;

    /**
     * @param beanContext The bean context the interceptor classes are beans of
     * @param enablements What decides whether an interceptor class a binding binds is enabled
     */
    InterceptorDefinitions(BeanContext beanContext, List<BoundInterceptorEnablement> enablements) {
        this.beanContext = beanContext;
        this.enablements = List.copyOf(enablements);
    }

    /**
     * Finds the interceptor classes whose binding annotations the element declares as well. The specification binds
     * an interceptor to an element when every binding of the interceptor is a binding of the element.
     *
     * <p>The processor recorded the bindings of both as the values of their binding annotations, reduced to what
     * they bind by. The comparison is Micronaut's exact interceptor binding rule,
     * {@code ExactInterceptorBindingQualifier}, which {@link InterceptorBindings#isBoundBy(InterceptorBindings)}
     * applies: an interceptor is bound when every binding it declares matches one of the element, member by member
     * through {@code AnnotationValue.matches}. That qualifier reads the bindings of a candidate from the
     * {@code io.micronaut.aop.InterceptorBinding} values of its metadata, which an interceptor class of this module
     * does not carry, so the candidates are filtered with {@link BoundInterceptorQualifier}, which hands the rule the
     * bindings the processor recorded on each of them instead.</p>
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
     *
     * <p>It is the selection {@link InterceptorBindings#boundInterceptors(BeanContext, InterceptorBindings)} makes
     * for another module, which asks it of this resolver so that the two cannot differ.</p>
     *
     * @param bindingsOfElement The bindings of the interception point
     * @return The definitions that describe the interceptor classes bound to the point, in the order the chain
     * invokes them in
     */
    List<BeanDefinition<?>> boundInterceptors(InterceptorBindings bindingsOfElement) {
        if (bindingsOfElement.isEmpty()) {
            return List.of();
        }
        // a class a factory produces as well is indexed by the definition of the factory too, which carries what the
        // processor recorded on the class; both are described by the one definition, and the class is bound once
        Set<BeanDefinition<?>> matching = new LinkedHashSet<>();
        for (BeanDefinition<JakartaInterceptorIndex> bound : beanContext.getBeanDefinitions(
            JakartaInterceptorIndex.class, new BoundInterceptorQualifier<>(bindingsOfElement, this::bindingsOf))) {
            BeanDefinition<?> definition = describing(bound.getBeanType());
            // a class that declares an interceptor method without declaring itself an interceptor class is one
            // only where it is named directly, and takes no part in what a binding annotation binds
            if (definition != null && definition.hasAnnotation(JakartaInterceptorSupport.INTERCEPTOR)
                && isEnabled(definition)) {
                matching.add(definition);
            }
        }
        List<BeanDefinition<?>> byPriority = new ArrayList<>(matching);
        byPriority.sort(Comparator.<BeanDefinition<?>>comparingInt(InterceptorMethods::priorityOf)
            .thenComparing(definition -> definition.getBeanType().getName()));
        return List.copyOf(listedLast(byPriority));
    }

    /**
     * The bindings an interceptor class declares, read off its definition once rather than for every chain that is
     * resolved: they are what the processor recorded on the definition, which does not change. A definition
     * Micronaut compiled is equal to every other instance of its class, so each is read once.
     */
    private InterceptorBindings bindingsOf(BeanType<?> interceptor) {
        return interceptorBindings.computeIfAbsent(interceptor, InterceptorBindings::of);
    }

    /**
     * Moves the interceptor classes a module orders by a list after the ones ordered by their priority, in the
     * order of the list. On its own the module has no such list, and the order by priority stands.
     */
    private List<BeanDefinition<?>> listedLast(List<BeanDefinition<?>> byPriority) {
        if (enablements.isEmpty()) {
            return byPriority;
        }
        Map<BeanDefinition<?>, Integer> positions = new HashMap<>();
        for (BeanDefinition<?> definition : byPriority) {
            for (BoundInterceptorEnablement enablement : enablements) {
                int position = enablement.position(definition);
                if (position >= 0) {
                    positions.put(definition, position);
                    break;
                }
            }
        }
        if (positions.isEmpty()) {
            return byPriority;
        }
        // a stable sort: the unlisted ones, at no position, come first in the order their priority and name gave
        // them, and two of one position stay in that order as well
        byPriority.sort(Comparator.comparingInt(definition -> positions.getOrDefault(definition, -1)));
        return byPriority;
    }

    /**
     * Whether an interceptor class a binding binds takes part in interception: every one does, unless a module
     * that has rules of enablement provided a {@link BoundInterceptorEnablement} that leaves it out.
     */
    private boolean isEnabled(BeanDefinition<?> definition) {
        for (BoundInterceptorEnablement enablement : enablements) {
            if (!enablement.isEnabled(definition)) {
                return false;
            }
        }
        return true;
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
    @Nullable BeanDefinition<?> describing(Class<?> interceptorClass) {
        Optional<BeanDefinition<?>> described = describing.get(interceptorClass);
        if (described == null) {
            // looking the definitions of the class up evaluates their conditions, and a condition may create a bean
            // that is intercepted, which resolves a chain and asks this again. Doing it inside a computeIfAbsent
            // would hold a lock of the map across the bean context and fail that second ask as a recursive update;
            // two threads describing one class find the same definition, so the later put loses nothing
            described = Optional.ofNullable(describe(interceptorClass));
            describing.put(interceptorClass, described);
        }
        return described.orElse(null);
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
    BeanDefinition<?> requireDescribing(Class<?> interceptorClass) {
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
            // a subclass that is an interceptor class too is a bean of this type as well, and describes itself:
            // its bindings and its interceptor methods are not those of the class asked about
            if (!isOfClass(definition, interceptorClass)
                || definition.getAnnotation(JakartaInterceptorMethods.class) == null) {
                continue;
            }
            // a bean a factory produces carries what its class records as well, but a proxy of it exposes the
            // methods it advises rather than the interceptor methods, so the definition kept is the one that has
            // an executable method for each of them
            if (hasRecordedMethods(definition)) {
                return definition;
            }
            fallback = definition;
        }
        return fallback;
    }

    /**
     * Whether a bean is a bean of the class itself rather than of a subclass of it. A lookup by type finds both,
     * and a subclass is another bean: what it declares is not what the class declares.
     *
     * <p>A bean Micronaut proxies is of the class when the class is the one it proxies.</p>
     *
     * @param bean The bean
     * @param type The class
     * @return Whether the bean is of the class
     */
    static boolean isOfClass(BeanType<?> bean, Class<?> type) {
        return bean.getBeanType() == type
            || bean instanceof AdvisedBeanType<?> advised && advised.getInterceptedType() == type
            || bean instanceof ProxyBeanDefinition<?> proxy && proxy.getTargetType() == type;
    }

    /**
     * Whether a definition has an executable method for every interceptor method recorded on it, which the
     * definition Micronaut generated from the class does and another definition of the class need not.
     */
    private static boolean hasRecordedMethods(BeanDefinition<?> definition) {
        AnnotationValue<JakartaInterceptorMethods> recorded = definition.getAnnotation(JakartaInterceptorMethods.class);
        if (recorded == null || definition.getExecutableMethods().isEmpty()) {
            return false;
        }
        for (InterceptionKind kind : InterceptionKind.values()) {
            for (String name : recorded.stringValues(kind.member())) {
                boolean found = definition.findPossibleMethods(name).anyMatch(method -> {
                    Class<?>[] argumentTypes = method.getArgumentTypes();
                    return argumentTypes.length == 1 && argumentTypes[0] == InvocationContext.class;
                });
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }
}
