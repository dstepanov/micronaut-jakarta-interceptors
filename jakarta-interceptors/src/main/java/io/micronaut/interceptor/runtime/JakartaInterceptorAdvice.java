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

import io.micronaut.aop.ConstructorInterceptor;
import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.InterceptorBinding;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.aop.InvocationContext;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.BeanResolutionContext;
import io.micronaut.context.DefaultBeanContext;
import io.micronaut.context.DefaultBeanResolutionContext;
import io.micronaut.context.DependentBeanProvider;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.exceptions.NonUniqueBeanException;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanConstructor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.annotation.JakartaInterception;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The single Micronaut interceptor of the Jakarta Interceptors implementation.
 *
 * <p>Every element the specification intercepts is bound to this one advice, which resolves the interceptor classes
 * that apply to it and runs them as one chain. Whatever the chain proceeds into - another Micronaut interceptor, or
 * the intercepted element itself - runs after all of them, which is the order the specification asks for.</p>
 *
 * <p>The advice is created for each object it intercepts rather than shared, because the interceptor instances it
 * holds belong to that one object: an interceptor may keep state for the life of the object it intercepts. Micronaut
 * creates it as a dependent of that object, uses the same advice for the construction, the lifecycle events and the
 * business methods of the object - through a proxy with a separate target as well, whose {@code @Around} the
 * processor declares {@code lazyInterceptorsPerTarget = true} on - and destroys it with the object, after the
 * pre-destroy event. The interceptor instances go with it, as section 2.3 has them go.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Prototype
@Internal
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.AROUND)
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.AROUND_CONSTRUCT)
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.POST_CONSTRUCT)
@InterceptorBinding(value = JakartaInterception.class, kind = InterceptorKind.PRE_DESTROY)
public final class JakartaInterceptorAdvice implements MethodInterceptor<Object, Object>, ConstructorInterceptor<Object> {

    /**
     * The annotation that makes a bean of a custom scope resolve to a proxy over a target the scope keeps. Named
     * rather than referenced: it is declared by a module this one does not depend on.
     */
    private static final String SCOPED_PROXY = "io.micronaut.runtime.context.scope.ScopedProxy";

    private final InterceptorChainResolver resolver;
    private final BeanContext beanContext;
    private final Map<Class<?>, Object> instances = new HashMap<>(4);
    /**
     * The registrations of the instances that belong to the object alone, in the order they were created. An
     * interceptor with a scope of its own - a {@code @Singleton}, or a bean of a custom scope, shared by every object
     * it intercepts - is not among them: it is not the object's to destroy. See {@link #own}.
     */
    private final List<BeanRegistration<?>> owned = new ArrayList<>(2);
    /**
     * Whether the interceptor instances of the object have been created as the object was, see
     * {@link #createInterceptorInstancesOnce}.
     */
    private volatile boolean instancesCreated;

    /**
     * @param resolver    The resolver of the interceptor chains
     * @param beanContext The context the interceptor classes are beans of
     */
    public JakartaInterceptorAdvice(InterceptorChainResolver resolver, BeanContext beanContext) {
        this.resolver = resolver;
        this.beanContext = beanContext;
    }

    @Override
    public int getOrder() {
        return Interceptor.Priority.APPLICATION;
    }

    /**
     * Destroys the interceptor instances of the object this advice was created for.
     *
     * <p>Micronaut destroys this advice together with that object, as a dependent created for it alone, once the
     * pre-destroy event of the object has been intercepted, or skipped by an ordinary Micronaut interceptor of the
     * event that did not proceed.</p>
     */
    @PreDestroy
    void destroyInterceptorInstances() {
        destroy();
    }

    // implementing both MethodInterceptor and ConstructorInterceptor inherits two declarations of this method,
    // of which the constructor one returns a non-null instance; an intercepted method may return null
    @SuppressWarnings("NullAway")
    @Override
    public @Nullable Object intercept(InvocationContext<Object, Object> context) {
        if (context instanceof ConstructorInvocationContext<Object> constructorContext) {
            return intercept(constructorContext);
        }
        if (context instanceof MethodInvocationContext<Object, Object> methodContext) {
            return intercept(methodContext);
        }
        throw new IllegalArgumentException("Unsupported invocation context: " + context);
    }

    @Override
    public @Nullable Object intercept(MethodInvocationContext<Object, Object> context) {
        InterceptorKind kind = context.getKind();
        if (kind == InterceptorKind.POST_CONSTRUCT || kind == InterceptorKind.PRE_DESTROY) {
            return interceptLifecycle(context, kind);
        }
        createInterceptorInstancesOnce(context);
        List<InterceptorReference> chain = resolver.resolve(kind, context.getAnnotationMetadata());
        if (chain.isEmpty()) {
            return context.proceed();
        }
        try {
            return new BusinessMethodInvocationContext(context, chain, this).proceed();
        } catch (Exception e) {
            throw sneakyThrow(e);
        }
    }

    /**
     * Creates the interceptor instances of the object on the first intercepted call, when nothing created them as the
     * object was created.
     *
     * <p>A proxy that resolves its target lazily - {@code @Around(proxyTarget = true, lazy = true)}, or a bean of a
     * scope that resolves through a proxy - has no target yet when it is created, so
     * {@link InterceptorCreationListener} finds no advice to ask, and the advice is only resolved for the target as the first call reaches it. Each method
     * interceptor would then be created only once its own method is called. The first call is the earliest the advice
     * of such a target is seen, so the instances of every method are created then, from the definition the listener
     * would have been handed: the one the target's class resolves to.</p>
     */
    private void createInterceptorInstancesOnce(MethodInvocationContext<Object, Object> context) {
        if (instancesCreated) {
            return;
        }
        synchronized (this) {
            if (instancesCreated) {
                return;
            }
            instancesCreated = true;
        }
        BeanDefinition<?> definition;
        try {
            definition = beanContext.findBeanDefinition(context.getTarget().getClass()).orElse(null);
        } catch (NonUniqueBeanException e) {
            // the class is ambiguous: each method interceptor is still created as its method is first called
            return;
        }
        if (definition != null) {
            createInterceptorInstances(definition);
        }
    }

    /**
     * Interposes on a {@code @PostConstruct} or {@code @PreDestroy} event of the object this advice was created for.
     *
     * <p>Micronaut invokes this once for the event, and proceeding the chain runs every callback of the bean in
     * order, superclass first - which is the interception the specification describes. An interceptor that does
     * not proceed keeps all of them from running.</p>
     */
    private @Nullable Object interceptLifecycle(MethodInvocationContext<Object, Object> context, InterceptorKind kind) {
        List<InterceptorReference> chain = resolver.resolve(kind, context.getAnnotationMetadata());
        if (chain.isEmpty()) {
            context.proceed();
            return context.getTarget();
        }
        LifecycleInvocationContext invocation = new LifecycleInvocationContext(context, chain, this);
        try {
            invocation.proceed();
        } catch (Exception e) {
            throw lifecycleFailure(e);
        }
        // the chain of a lifecycle callback carries the bean itself, which an interceptor may neither replace nor
        // discard: an interceptor that does not proceed only keeps the rest of the chain from running
        return context.getTarget();
    }

    @Override
    public Object intercept(ConstructorInvocationContext<Object> context) {
        BeanConstructor<Object> constructor = context.getConstructor();
        // the annotation metadata of a constructor invocation is carried by the constructor rather than by the
        // context, which has none of its own
        List<InterceptorReference> chain =
            resolver.resolve(InterceptorKind.AROUND_CONSTRUCT, constructor.getAnnotationMetadata());
        if (chain.isEmpty()) {
            return context.proceed();
        }
        ConstructorInvocationContextAdapter invocation = new ConstructorInvocationContextAdapter(
            context, chain, this, associatedWithTheClass(constructor.getAnnotationMetadata()));
        try {
            invocation.proceed();
        } catch (Exception e) {
            // unlike a lifecycle callback, an around-construct interceptor method may throw what the constructor
            // declares, and lets through what the constructor throws. Both leave as they are, so that Micronaut
            // reports them as it reports a constructor that is not intercepted: a runtime exception of the advice
            // reaches the caller unchanged, and anything else is the cause of the failure to create the bean
            throw sneakyThrow(e);
        }
        Object constructed = invocation.constructed();
        if (constructed == null) {
            throw new IllegalStateException("An @AroundConstruct interceptor of ["
                + constructor.getDeclaringBeanType().getName() + "] returned without calling "
                + "InvocationContext.proceed(), so no instance was created");
        }
        return constructed;
    }

    /**
     * The interceptor classes the class of an object being constructed is bound to, other than those of its
     * around-construct chain.
     *
     * <p>Section 2.3 da) has injection completed on the instances of all the interceptor classes associated with the
     * target class before an {@code @AroundConstruct} method runs, so an interceptor class that declares nothing but
     * an {@code @AroundInvoke} method, and therefore takes no part in the construction, is created then too. The
     * association rules of the construction itself are untouched: what is created here interposes on nothing, and the
     * chain the construction runs is still only the one resolved for it.</p>
     *
     * <p>Read from what the processor records on the constructor apart from its chain: the interceptor classes the
     * class names, whether or not the constructor excludes them with {@code @ExcludeClassInterceptors}, and the
     * bindings in effect on it. Every kind of interceptor method counts, an {@code @AroundTimeout} one included. An
     * interceptor class bound to one method of the object alone is not among them: the methods of a bean are not reachable from a
     * constructor interception, and it is created as the object finishes being created. See the guide.</p>
     *
     * @param metadata The annotation metadata of the constructor
     * @return The interceptors, which may repeat those of the chain
     */
    private List<InterceptorReference> associatedWithTheClass(AnnotationMetadata metadata) {
        return resolver.resolveAssociated(metadata);
    }

    /**
     * Creates now every interceptor instance that will interpose on the given object.
     *
     * <p>The specification creates an interceptor instance when the object it intercepts is created, whether or
     * not anything is ever invoked on that object, so that what an interceptor class does as it is constructed
     * happens then. Resolving a chain the first time it is proceeded would instead create the instances of a
     * method's interceptors only once that method is called, and never for a method nobody calls.</p>
     *
     * <p>Called by {@link InterceptorCreationListener} as the object is created, on the advice bound to it, which
     * is the advice whose instances every interception of that object goes on to use.</p>
     *
     * @param definition The definition of the object
     */
    void createInterceptorInstances(BeanDefinition<?> definition) {
        instancesCreated = true;
        AnnotationMetadata classMetadata = definition.getAnnotationMetadata();
        // only what this module intercepts is asked for: an element it does not intercept has no chain, and
        // resolving one would put an empty chain in the resolver's map for nothing
        if (classMetadata.hasAnnotation(JakartaInterception.class)) {
            createInstancesOf(InterceptorKind.POST_CONSTRUCT, classMetadata);
            createInstancesOf(InterceptorKind.PRE_DESTROY, classMetadata);
        }
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            AnnotationMetadata methodMetadata = method.getAnnotationMetadata();
            if (methodMetadata.hasAnnotation(JakartaInterception.class)) {
                createInstancesOf(InterceptorKind.AROUND, methodMetadata);
            }
        }
    }

    private void createInstancesOf(InterceptorKind kind, AnnotationMetadata metadata) {
        createInterceptorInstances(resolver.resolve(kind, metadata));
    }

    /**
     * Returns the instance of an interceptor class, creating it the first time it is asked for.
     *
     * @param reference The interceptor
     * @return The instance
     */
    synchronized Object interceptorInstance(InterceptorReference reference) {
        Class<?> interceptorClass = reference.interceptorClass();
        Object instance = instances.get(interceptorClass);
        if (instance == null) {
            instance = create(interceptorClass);
            instances.put(interceptorClass, instance);
        }
        return instance;
    }

    /**
     * Creates the instance of every interceptor class of the given references that has none yet.
     *
     * @param references The interceptors
     */
    void createInterceptorInstances(List<InterceptorReference> references) {
        for (InterceptorReference reference : references) {
            // an interceptor method the intercepted class declares itself runs on the object, and has no
            // instance of its own to create
            if (!reference.self()) {
                interceptorInstance(reference);
            }
        }
    }

    /**
     * Creates the instance of an interceptor class.
     *
     * <p>Resolved by type rather than from one definition: an interceptor class may also be produced by a factory,
     * and the instance the application configured there is the one to intercept with. Resolved in a resolution
     * context of its own, which is where Micronaut records what it created as a dependency rather than found in a
     * scope, exactly as it does for the dependencies of any bean, so nothing here has to know the scopes.</p>
     */
    private Object create(Class<?> interceptorClass) {
        if (!(beanContext instanceof DefaultBeanContext)) {
            // a context of another implementation records no dependents to read
            return beanContext.getBean(interceptorClass);
        }
        try (BeanResolutionContext resolutionContext = new DefaultBeanResolutionContext(beanContext, null)) {
            Object instance = resolutionContext.getBean(interceptorClass);
            for (BeanRegistration<?> registration : resolutionContext.getAndResetDependentBeans()) {
                own(registration);
            }
            return instance;
        }
    }

    /**
     * Keeps what a registration Micronaut reported as a dependent leaves the object to destroy.
     *
     * <p>A bean of a custom scope that resolves through a proxy is reported as that proxy, while the bean behind it
     * belongs to the scope and serves every object the interceptor is bound to: destroying the registration would
     * remove the bean from its scope. The dependents of the proxy are kept instead, which is what Micronaut destroys
     * when it destroys such a proxy as the dependent of a bean.</p>
     */
    private void own(BeanRegistration<?> registration) {
        BeanDefinition<?> definition = registration.getBeanDefinition();
        if (definition.isProxy() && definition.hasStereotype(SCOPED_PROXY)) {
            if (registration instanceof DependentBeanProvider provider) {
                for (BeanRegistration<?> dependent : provider.dependentBeans()) {
                    own(dependent);
                }
            }
            return;
        }
        owned.add(registration);
    }

    /**
     * Destroys the interceptor instances that belong to the object alone, latest first, as Micronaut destroys the
     * dependents of a bean, and forgets every instance. Destroying them twice destroys them once. One failing to be
     * destroyed does not keep the rest alive: the first failure is reported once all are destroyed.
     */
    private void destroy() {
        List<BeanRegistration<?>> destroyed;
        synchronized (this) {
            instances.clear();
            if (owned.isEmpty()) {
                return;
            }
            destroyed = new ArrayList<>(owned);
            owned.clear();
        }
        RuntimeException failure = null;
        // outside of the lock: what an interceptor does as it is destroyed is its own code
        for (int i = destroyed.size() - 1; i >= 0; i--) {
            try {
                beanContext.destroyBean(destroyed.get(i));
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Reports what a lifecycle callback interceptor method threw.
     *
     * <p>The specification lets such a method throw a runtime exception but not a checked one, and there is
     * nowhere for a checked exception to go: what invoked the callback is the container, and it declares none. A
     * runtime exception travels as it is, and anything else is wrapped in one.</p>
     *
     * @param e The exception
     * @return The exception to throw
     */
    private static RuntimeException lifecycleFailure(Exception e) {
        if (e instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException("A lifecycle callback interceptor method threw a checked exception, which "
            + "the specification does not allow it to throw: " + e, e);
    }

    /**
     * Rethrows the exception of an interceptor method as it is.
     *
     * <p>An interceptor method is declared to throw {@link Exception}, and the checked exceptions the intercepted
     * method itself declares travel through it. The intercepted method is what the caller sees, and it does declare
     * them, so the exception is rethrown unchanged rather than wrapped in something the caller cannot catch.</p>
     *
     * @param e   The exception
     * @param <E> The type the exception is rethrown as
     * @return Never returns; declared so that the call sites can be written as a {@code throw}
     * @throws E The exception
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
