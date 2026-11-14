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
import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.aop.MethodInvocationContext;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Resolves the chain of the construction of a bean, read from its constructor.
     *
     * <p>The chain is kept by the definition of the bean, see {@link ObjectChains}, which Micronaut hands the
     * context of the construction. A context that carries no definition has its chain built from the constructor
     * it carries, and kept nowhere.</p>
     *
     * @param context The context of the construction
     * @return The interceptors, in the order they are invoked in
     */
    List<InterceptorReference> resolveConstruction(ConstructorInvocationContext<?> context) {
        Optional<? extends BeanDefinition<?>> definition = context.findBeanDefinition();
        if (definition.isPresent()) {
            return objectChains(definition.get()).construction();
        }
        return construction(context.getConstructor().getAnnotationMetadata());
    }

    /**
     * The chains of a method invoked as a business method, or by the scheduler, which the method keeps, see
     * {@link MethodChains}.
     *
     * @param method The method
     * @return Its chains
     */
    MethodChains chainsOf(ExecutableMethod<?, ?> method) {
        return method.getCachedUnevaluatedValue(MethodChains.class, this::readChains);
    }

    /**
     * Resolves the chain of a lifecycle event of a bean.
     *
     * <p>Micronaut runs each event through a method object of its own, which keeps no value, but which hands out
     * the definition of the bean, and the chain is kept by that definition, see {@link ObjectChains}. The method
     * carries the metadata of the class, which is what the chain is built from where there is no definition.</p>
     *
     * @param kind    The lifecycle event, {@code POST_CONSTRUCT} or {@code PRE_DESTROY}
     * @param context The context of the event
     * @return The interceptors, in the order they are invoked in, none for a kind that is not a lifecycle event
     */
    List<InterceptorReference> resolveLifecycle(InterceptorKind kind, MethodInvocationContext<?, ?> context) {
        if (kind != InterceptorKind.POST_CONSTRUCT && kind != InterceptorKind.PRE_DESTROY) {
            return List.of();
        }
        Optional<? extends BeanDefinition<?>> definition = context.findBeanDefinition();
        if (definition.isPresent()) {
            ObjectChains chains = objectChains(definition.get());
            return kind == InterceptorKind.POST_CONSTRUCT ? chains.postConstruct() : chains.preDestroy();
        }
        InterceptionKind interceptionKind = kind == InterceptorKind.POST_CONSTRUCT
            ? InterceptionKind.POST_CONSTRUCT
            : InterceptionKind.PRE_DESTROY;
        return build(interceptionKind, recordedInterception(context.getExecutableMethod().getAnnotationMetadata()));
    }

    private ObjectChains objectChains(BeanDefinition<?> definition) {
        return definition.getCachedUnevaluatedValue(ObjectChains.class, this::readObjectChains);
    }

    /**
     * Builds what the definition keeps of the interception of the objects it creates: the chains of the
     * construction and of the two lifecycle events, and the interceptor classes whose instances are created with
     * an object.
     */
    private ObjectChains readObjectChains(BeanDefinition<?> definition) {
        List<InterceptorReference> construction = construction(definition.getConstructor().getAnnotationMetadata());
        AnnotationValue<JakartaInterception> classInterception =
            recordedInterception(definition.getAnnotationMetadata());
        return new ObjectChains(construction,
            build(InterceptionKind.POST_CONSTRUCT, classInterception),
            build(InterceptionKind.PRE_DESTROY, classInterception),
            interceptorClasses(definition, construction, classInterception));
    }

    private List<InterceptorReference> construction(AnnotationMetadata constructorMetadata) {
        return build(InterceptionKind.AROUND_CONSTRUCT, recordedInterception(constructorMetadata));
    }

    private MethodChains readChains(ExecutableMethod<?, ?> method) {
        AnnotationValue<JakartaInterception> interception = recordedInterception(method.getAnnotationMetadata());
        boolean timeout = interception != null && interception.booleanValue(JakartaInterception.TIMEOUT).orElse(false);
        List<InterceptorReference> business = build(InterceptionKind.AROUND_INVOKE, interception);
        return new MethodChains(business,
            timeout ? build(InterceptionKind.AROUND_TIMEOUT, interception) : business,
            timeout);
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
     * {@code @AroundTimeout} method of a bean nothing schedules. Every interceptor class the class names or binds is
     * therefore among those created, whatever kinds its interceptor methods interpose on: the processor rejects an
     * interceptor class that declares none.</p>
     *
     * <p>The classes of a method are the ones its chains run, which are selected rather than built here: the chains
     * of a method are kept by the method the proxy invokes, which is not among the methods of the definition, and
     * building them on those would build them twice.</p>
     *
     * <p>Only what this module intercepts is asked for: an element it does not intercept has no chain. An
     * interceptor method the class declares itself runs on the object, and has no instance of its own. The classes
     * are kept by the definition, with its chains, see {@link ObjectChains}, rather than read from every chain of the
     * object again for every bean of it created.</p>
     *
     * @param definition The definition of the object
     * @return The interceptor classes, each once, in the order they are first met
     */
    List<Class<?>> resolveInterceptorClasses(BeanDefinition<?> definition) {
        return objectChains(definition).interceptorClasses();
    }

    private List<Class<?>> interceptorClasses(BeanDefinition<?> definition,
                                              List<InterceptorReference> construction,
                                              @Nullable AnnotationValue<JakartaInterception> classInterception) {
        Set<Class<?>> classes = new LinkedHashSet<>();
        addInterceptorClasses(construction, classes);
        for (BeanDefinition<?> interceptor : selected(classInterception)) {
            classes.add(interceptor.getBeanType());
        }
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            AnnotationValue<JakartaInterception> interception = recordedInterception(method.getAnnotationMetadata());
            boolean timeout = interception != null
                && interception.booleanValue(JakartaInterception.TIMEOUT).orElse(false);
            for (BeanDefinition<?> interceptor : selected(interception)) {
                // the classes the chains of the method run: its business chain, of the @AroundInvoke methods, and
                // the chain of a timeout, of the @AroundTimeout methods or, without any, the @AroundInvoke ones
                if (!InterceptorMethods.of(interceptor, InterceptionKind.AROUND_INVOKE).isEmpty()
                    || timeout && !InterceptorMethods.of(interceptor, InterceptionKind.AROUND_TIMEOUT).isEmpty()) {
                    classes.add(interceptor.getBeanType());
                }
            }
        }
        return List.copyOf(classes);
    }

    private static void addInterceptorClasses(List<InterceptorReference> chain, Set<Class<?>> classes) {
        for (InterceptorReference reference : chain) {
            if (!reference.self()) {
                classes.add(reference.interceptorClass());
            }
        }
    }

    /**
     * Builds a chain from the interception the processor recorded on an element and the kind of interception, which
     * is everything it is built from.
     */
    private List<InterceptorReference> build(InterceptionKind kind,
                                             @Nullable AnnotationValue<JakartaInterception> interception) {
        if (interception == null || interception.booleanValue(JakartaInterception.EXCLUDED).orElse(false)) {
            return List.of();
        }
        Collection<BeanDefinition<?>> ordered = selected(interception);
        // the interceptor methods a class declares on itself are invoked after every interceptor class
        Class<?> self = interception.classValue(JakartaInterception.SELF).filter(type -> type != void.class).orElse(null);
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
     * The interceptor classes an interception names and binds, in the order their interceptor methods are invoked in,
     * whatever kind of interception they interpose on.
     */
    private Collection<BeanDefinition<?>> selected(@Nullable AnnotationValue<JakartaInterception> interception) {
        // an element that records no interception names no interceptor class and has no binding to be bound by
        if (interception == null || interception.booleanValue(JakartaInterception.EXCLUDED).orElse(false)) {
            return List.of();
        }
        return ordered(
            List.of(interception.classValues(JakartaInterception.INTERCEPTORS)),
            InterceptorBindings.ofValues(interception.getAnnotations(JakartaInterception.BINDINGS)));
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
     * <p>A set of the definitions describing them keeps the order while making sure an interceptor class that is
     * both named directly and bound by an annotation is only invoked once, at its first position. The definition
     * rather than its bean type tells the two apart: an interceptor class Micronaut proxies is described by the
     * definition of the subclass it generates, whose bean type is not the class the element names.</p>
     */
    private Collection<BeanDefinition<?>> ordered(List<Class<?>> interceptors, InterceptorBindings bindings) {
        Set<BeanDefinition<?>> ordered = new LinkedHashSet<>();
        for (Class<?> interceptorClass : interceptors) {
            ordered.add(definitions.requireDescribing(interceptorClass));
        }
        // the definitions a binding selects are the ones describing the interceptor classes already
        ordered.addAll(definitions.boundInterceptors(bindings));
        return ordered;
    }

    /**
     * The interceptor methods of one interceptor class that interpose on a kind of interception, in the order the
     * specification invokes them: the ones its superclasses declare first, its own last.
     */
    private static List<InterceptorReference> references(BeanDefinition<?> definition, InterceptionKind kind, boolean self) {
        // the interceptor methods themselves are kept by the definition, see InterceptorMethods.of
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
        return references;
    }
}
