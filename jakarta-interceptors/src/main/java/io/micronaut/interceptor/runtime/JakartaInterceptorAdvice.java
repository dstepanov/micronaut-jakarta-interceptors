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
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanConstructor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.annotation.JakartaInterception;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.Interceptor;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The single Micronaut interceptor of the Jakarta Interceptors implementation.
 *
 * <p>Every element the specification intercepts is bound to this one advice, which resolves the interceptor classes
 * that apply to it and runs them as one chain. Whatever the chain proceeds into - another Micronaut interceptor, or
 * the intercepted element itself - runs after all of them, which is the order the specification asks for.</p>
 *
 * <p>The advice is created for each object it intercepts rather than shared, because the interceptor instances it
 * holds belong to that one object: an interceptor may keep state for the life of the object it intercepts.</p>
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

    private final InterceptorChainResolver resolver;
    private final InterceptorInstances instances;
    // one for each lifecycle event of the object this advice was created for

    /**
     * @param resolver    The resolver of the interceptor chains
     * @param beanContext The context the interceptor classes are beans of
     */
    public JakartaInterceptorAdvice(InterceptorChainResolver resolver, BeanContext beanContext) {
        this.resolver = resolver;
        this.instances = new InterceptorInstances(beanContext);
    }

    @Override
    public int getOrder() {
        return Interceptor.Priority.APPLICATION;
    }

    /**
     * Destroys the interceptor instances of the object this advice was created for, unless the pre-destroy event of
     * that object is what destroys them.
     *
     * <p>Micronaut destroys this advice together with that object, as a dependent created for it alone, and the
     * interceptor instances go with it: section 2.3 gives them the life of the object they intercept. See
     * {@link InterceptorInstances#adviceDestroyed()} for the objects whose instances wait for their pre-destroy
     * event instead.</p>
     */
    @PreDestroy
    void destroyInterceptorInstances() {
        instances.adviceDestroyed();
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
        if (kind == InterceptorKind.POST_CONSTRUCT) {
            instances.postConstructed();
            return interceptLifecycle(context, kind);
        }
        if (kind == InterceptorKind.PRE_DESTROY) {
            try {
                return interceptLifecycle(context, kind);
            } finally {
                // 2.3 cc): the interceptor instances are destroyed once the pre-destroy interception is over
                instances.preDestroyed();
            }
        }
        List<InterceptorReference> chain = resolver.resolve(keyOf(context, kind), context.getAnnotationMetadata());
        if (chain.isEmpty()) {
            return context.proceed();
        }
        try {
            return new BusinessMethodInvocationContext(context, chain, instances).proceed();
        } catch (Exception e) {
            throw sneakyThrow(e);
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
        List<InterceptorReference> chain = resolver.resolve(keyOf(context, kind), context.getAnnotationMetadata());
        if (chain.isEmpty()) {
            context.proceed();
            return context.getTarget();
        }
        LifecycleInvocationContext invocation = new LifecycleInvocationContext(context, chain, instances);
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
        InterceptorChainResolver.ChainKey key = new InterceptorChainResolver.ChainKey(
            constructor.getDeclaringBeanType(), InterceptorKind.AROUND_CONSTRUCT);
        // the annotation metadata of a constructor invocation is carried by the constructor rather than by the
        // context, which has none of its own
        List<InterceptorReference> chain = resolver.resolve(key, constructor.getAnnotationMetadata());
        if (chain.isEmpty()) {
            return context.proceed();
        }
        ConstructorInvocationContextAdapter invocation =
            new ConstructorInvocationContextAdapter(context, chain, instances);
        try {
            invocation.proceed();
        } catch (Exception e) {
            throw lifecycleFailure(e);
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
        Class<?> targetType = definition.getBeanType();
        AnnotationMetadata classMetadata = definition.getAnnotationMetadata();
        // only what this module intercepts is asked for: an element it does not intercept has no chain, and
        // resolving one would put an empty chain in the resolver's map for nothing
        if (classMetadata.hasAnnotation(JakartaInterception.class)) {
            createInstancesOf(new InterceptorChainResolver.ChainKey(targetType, InterceptorKind.POST_CONSTRUCT), classMetadata);
            createInstancesOf(new InterceptorChainResolver.ChainKey(targetType, InterceptorKind.PRE_DESTROY), classMetadata);
        }
        for (ExecutableMethod<?, ?> method : definition.getExecutableMethods()) {
            AnnotationMetadata methodMetadata = method.getAnnotationMetadata();
            if (methodMetadata.hasAnnotation(JakartaInterception.class)) {
                createInstancesOf(new InterceptorChainResolver.ChainKey(method, InterceptorKind.AROUND), methodMetadata);
            }
        }
    }

    private void createInstancesOf(InterceptorChainResolver.ChainKey key, AnnotationMetadata metadata) {
        for (InterceptorReference reference : resolver.resolve(key, metadata)) {
            // an interceptor method the intercepted class declares itself runs on the object, and has no
            // instance of its own to create
            if (!reference.self()) {
                instances.get(reference);
            }
        }
    }

    private static InterceptorChainResolver.ChainKey keyOf(MethodInvocationContext<Object, Object> context,
                                                           InterceptorKind kind) {
        return switch (kind) {
            // a lifecycle chain belongs to the bean, which has one of each kind. It is not identified by the
            // callback the chain is run for: two beans bound to different interceptors may inherit the callback
            // the chain starts at from the same superclass
            case POST_CONSTRUCT, PRE_DESTROY -> new InterceptorChainResolver.ChainKey(targetTypeOf(context), kind);
            default -> new InterceptorChainResolver.ChainKey(context.getExecutableMethod(), kind);
        };
    }

    /**
     * The class of the object being intercepted, which for a bean Micronaut also generated a proxy of is that
     * proxy. Either way it is one class for one bean, which is what a lifecycle chain is resolved for.
     */
    private static Class<?> targetTypeOf(MethodInvocationContext<Object, Object> context) {
        Object target = context.getTarget();
        return target == null ? context.getExecutableMethod().getDeclaringType() : target.getClass();
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
