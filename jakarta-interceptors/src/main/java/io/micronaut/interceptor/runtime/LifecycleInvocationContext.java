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

import io.micronaut.aop.Interceptor;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.MicronautMethodInvocationContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The {@code InvocationContext} of a {@code @PostConstruct} or {@code @PreDestroy} interceptor method.
 *
 * <p>The specification requires {@code getParameters()} to fail for a lifecycle callback, which the base class
 * already does. What is left is the target, which exists for the whole of the callback, the callback of the
 * intercepted class itself, and the description of the element for the error messages.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class LifecycleInvocationContext extends AbstractInvocationContext
    implements MicronautMethodInvocationContext {

    private final MethodInvocationContext<Object, ?> context;
    private @Nullable Method method;
    private boolean methodResolved;
    private boolean proceeding;

    LifecycleInvocationContext(MethodInvocationContext<Object, ?> context,
                               List<InterceptorReference> chain,
                               InterceptorInstances instances,
                               Interceptor<?, ?> advice) {
        super(context, chain, instances, advice);
        this.context = context;
    }

    @Override
    public Object getTarget() {
        return context.getTarget();
    }

    @Override
    public InterceptorKind getInterceptorKind() {
        return context.getKind();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ExecutableMethod<Object, Object> getExecutableMethod() {
        return (ExecutableMethod<Object, Object>) context.getExecutableMethod();
    }

    @SuppressWarnings("unchecked")
    @Override
    public MethodInvocationContext<Object, Object> getMicronautInvocation() {
        return (MethodInvocationContext<Object, Object>) context;
    }

    /**
     * The specification shows a lifecycle callback interceptor method the callback of the intercepted class, and
     * {@code null} only when the class declares none.
     *
     * <p>That is what the executable method of the interception already is. A chain runs for the event rather than
     * for a callback, and it describes itself by the last callback it invokes - the one the intercepted class
     * declares - so the method of the specification and the method of the interception are the same. A bean bound
     * for a kind it declares no callback of has the event itself, whose target method is {@code null}, which is
     * the answer the specification asks for there.</p>
     *
     * <p>Resolving it is the one place a lifecycle interception reaches for the reflection of the platform, and it
     * only happens when an interceptor asks for the method. It used to be a walk up the class hierarchy, looking
     * for a name the processor had recorded, which read the declared methods of every class between the bean and
     * the one that declared the callback. Micronaut answers it in one lookup.</p>
     *
     * @return The callback of the intercepted class, or {@code null} when it declares none
     */
    @Override
    public @Nullable Method getMethod() {
        if (!methodResolved) {
            methodResolved = true;
            // getMethod returns a java.lang.reflect.Method, which only the platform can produce
            @SuppressWarnings("NoReflection")
            @Nullable Method target = context.getExecutableMethod().getTargetMethod();
            method = target;
        }
        return method;
    }

    /**
     * Runs the chain, and destroys the interceptor instances of the object when its post-construct event fails.
     *
     * <p>An exception that leaves the post-construct chain fails the creation of the object, and section 2.3 has the
     * interceptor instances of an object that fails to be created destroyed. Every interceptor of the chain proceeds
     * through this same context, so it is the outermost call - the one the advice makes - that decides: an exception
     * an interceptor catches on the way out fails nothing. A pre-destroy event that fails still ends with the object
     * destroyed, and its interceptor instances with it, so there is nothing to do for one here.</p>
     *
     * @return What the chain returned
     * @throws Exception What the chain threw
     */
    @Override
    public @Nullable Object proceed() throws Exception {
        if (proceeding || context.getKind() != InterceptorKind.POST_CONSTRUCT) {
            return super.proceed();
        }
        proceeding = true;
        try {
            return super.proceed();
        } catch (Throwable e) {
            discardInterceptorInstances();
            throw e;
        } finally {
            proceeding = false;
        }
    }

    /**
     * A lifecycle callback returns nothing, and the specification has the {@code proceed} of the last interceptor
     * of the chain return {@code null} rather than anything the callback of the bean produced. The chain is still
     * proceeded into: what the bean returns is simply not the interceptor's to see.
     *
     * @return Always {@code null}
     */
    @Override
    @Nullable Object proceedTarget() {
        super.proceedTarget();
        return null;
    }

    @Override
    String description() {
        return "the " + context.getKind() + " callback of " + context.getTarget().getClass().getName();
    }
}
