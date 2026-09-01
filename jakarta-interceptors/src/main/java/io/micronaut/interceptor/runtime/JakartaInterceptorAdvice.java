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
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanConstructor;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.annotation.JakartaInterception;
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
    private final InterceptorLifecycleSupport lifecycle;
    private volatile @org.jspecify.annotations.Nullable InterceptorInstances instances;
    private final BeanContext beanContext;

    /**
     * @param resolver    The resolver of the interceptor chains
     * @param lifecycle   The lifecycle of the interceptor instances
     * @param beanContext The context the interceptor classes are beans of
     */
    public JakartaInterceptorAdvice(InterceptorChainResolver resolver, InterceptorLifecycleSupport lifecycle,
                                    BeanContext beanContext) {
        this.resolver = resolver;
        this.lifecycle = lifecycle;
        this.beanContext = beanContext;
    }

    @Override
    public int getOrder() {
        return Interceptor.Priority.APPLICATION;
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
        List<InterceptorReference> chain = resolver.resolve(keyOf(context, kind), context.getAnnotationMetadata());
        if (chain.isEmpty()) {
            return context.proceed();
        }
        InterceptorInstances instances = instancesOf(context.getTarget());
        AbstractInvocationContext invocation = switch (kind) {
            case POST_CONSTRUCT, PRE_DESTROY -> new LifecycleInvocationContext(context, chain, instances);
            default -> new BusinessMethodInvocationContext(context, chain, instances);
        };
        Object result;
        try {
            result = invocation.proceed();
        } catch (Exception e) {
            throw kind == InterceptorKind.AROUND ? sneakyThrow(e) : lifecycleFailure(e);
        }
        // the chain of a lifecycle callback carries the bean itself, which an interceptor may neither replace nor
        // discard: an interceptor that does not proceed only keeps the rest of the chain from running
        return kind == InterceptorKind.AROUND ? result : context.getTarget();
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
        // the object does not exist yet, so its instances cannot be looked up by it: they are created here and
        // handed over once the constructor has run, so that an interceptor class interposing on both the
        // construction and the methods of the object is one instance
        InterceptorInstances constructing = instancesOf(null);
        ConstructorInvocationContextAdapter invocation =
            new ConstructorInvocationContextAdapter(context, chain, constructing);
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
        // the object exists now: the instances created for its construction are the ones its methods and its
        // pre-destroy see, because this advice is one object per intercepted object
        lifecycle.hold(constructed, constructing);
        return constructed;
    }

    /**
     * The interceptor instances of the object this advice interposes on. One advice object serves one
     * intercepted object — the construction of it, its methods and its callbacks — so they are held here.
     */
    private InterceptorInstances instancesOf(@org.jspecify.annotations.Nullable Object target) {
        InterceptorInstances held = instances;
        if (held == null) {
            synchronized (this) {
                held = instances;
                if (held == null) {
                    held = target == null ? new InterceptorInstances(beanContext)
                        : lifecycle.instancesFor(target);
                    instances = held;
                }
            }
        }
        return held;
    }

    private static InterceptorChainResolver.ChainKey keyOf(MethodInvocationContext<Object, Object> context,
                                                           InterceptorKind kind) {
        ExecutableMethod<Object, Object> method = context.getExecutableMethod();
        return switch (kind) {
            // the executable method of a lifecycle callback is a new object for every bean, so the class is what
            // identifies the chain there; a bean has one callback chain of each kind anyway
            case POST_CONSTRUCT, PRE_DESTROY -> new InterceptorChainResolver.ChainKey(method.getDeclaringType(), kind);
            default -> new InterceptorChainResolver.ChainKey(method, kind);
        };
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
