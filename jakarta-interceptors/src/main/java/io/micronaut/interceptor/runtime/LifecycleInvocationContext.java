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

import io.micronaut.aop.Intercepted;
import io.micronaut.aop.InterceptorKind;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.MicronautMethodInvocationContext;
import io.micronaut.interceptor.annotation.JakartaInterception;
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
    private boolean reachedBean;

    LifecycleInvocationContext(MethodInvocationContext<Object, ?> context,
                               List<InterceptorReference> chain,
                               InterceptorInstances instances) {
        super(context, chain, instances);
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

    /**
     * The specification shows a lifecycle callback interceptor method the callback of the intercepted class, and
     * {@code null} only when the class declares none. Which method that is was worked out at compilation time and
     * recorded on the class; looking it up is the only reflection of a lifecycle interception, and it only happens
     * when an interceptor asks for the method.
     *
     * <p>Micronaut interposes on the lifecycle of a bean rather than on one callback of it, so what is looked up
     * is the callback of the intercepted class itself. It is found by its name, walking up from the class: a class
     * declares at most one callback of a kind, and the name is the one the processor read from that declaration.</p>
     *
     * @return The callback of the intercepted class, or {@code null} when it declares none
     */
    @Override
    public @Nullable Method getMethod() {
        if (!methodResolved) {
            methodResolved = true;
            method = resolveCallback();
        }
        return method;
    }

    private @Nullable Method resolveCallback() {
        String member = context.getKind() == InterceptorKind.PRE_DESTROY ? "preDestroy" : "postConstruct";
        String name = getAnnotationMetadata().stringValue(JakartaInterception.class, member).orElse(null);
        if (name == null || name.isEmpty()) {
            return null;
        }
        Class<?> targetClass = context.getTarget().getClass();
        if (Intercepted.class.isAssignableFrom(targetClass)) {
            // what the bean context holds is the proxy Micronaut generated, whose class is a subclass of the one
            // that declared the callback
            Class<?> superclass = targetClass.getSuperclass();
            if (superclass != null) {
                targetClass = superclass;
            }
        }
        for (Class<?> type = targetClass; type != null; type = type.getSuperclass()) {
            try {
                // the specification gives the callback of a class the signature void <METHOD>()
                return type.getDeclaredMethod(name);
            } catch (NoSuchMethodException e) {
                Method callback = declaredMethodNamed(type, name);
                if (callback != null) {
                    return callback;
                }
            }
        }
        return null;
    }

    /**
     * The callback of a class that takes something: Micronaut injects into a lifecycle callback, which the
     * specification does not describe, so the method is found by its name alone. A class declares at most one
     * callback of a kind, so there is nothing else of that name to find.
     */
    private static @Nullable Method declaredMethodNamed(Class<?> type, String name) {
        for (Method candidate : type.getDeclaredMethods()) {
            if (candidate.getName().equals(name) && !candidate.isSynthetic()) {
                return candidate;
            }
        }
        return null;
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
        reachedBean = true;
        super.proceedTarget();
        return null;
    }

    /**
     * Whether the chain was proceeded all the way into the bean.
     *
     * <p>Micronaut interposes on each callback of a lifecycle event separately, while one chain of interceptor
     * methods runs for the whole event. The callbacks after the first are invoked without a chain of their own,
     * and only when this chain reached the bean at all: an interceptor that does not proceed keeps every callback
     * of the event from running, not only the one its chain was started for.</p>
     *
     * @return Whether the bean was reached
     */
    boolean reachedBean() {
        return reachedBean;
    }

    @Override
    String description() {
        return "the " + context.getKind() + " callback of " + context.getTarget().getClass().getName();
    }
}
