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
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.interceptor.MicronautMethodInvocationContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The {@code InvocationContext} of an {@code @AroundInvoke} interceptor method.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class BusinessMethodInvocationContext extends AbstractInvocationContext
    implements MicronautMethodInvocationContext {

    private final MethodInvocationContext<Object, ?> context;
    private @Nullable Method method;
    private @Nullable Object timer;
    private boolean timerResolved;

    BusinessMethodInvocationContext(MethodInvocationContext<Object, ?> context,
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
     * The specification hands the interceptor a {@link Method}, so one is looked up. That lookup is the only
     * reflection of an {@code @AroundInvoke} interception, and it only happens when an interceptor asks for the
     * method: the invocation itself goes through the executable method Micronaut generated at compilation time.
     *
     * @return The intercepted method
     */
    @Override
    public Method getMethod() {
        Method resolved = method;
        if (resolved == null) {
            resolved = context.getExecutableMethod().getTargetMethod();
            method = resolved;
        }
        return resolved;
    }

    /**
     * The specification returns the timer of the timer service that invoked a timeout method. What stands for one
     * here is the schedule the method was registered with, and an ordinary business method has none.
     *
     * @return The schedule of the method, or {@code null} when the scheduler does not invoke it
     */
    @Override
    public @Nullable Object getTimer() {
        if (!timerResolved) {
            timerResolved = true;
            // a schedule is recorded through its repeatable container even when a method declares only one
            timer = context.getAnnotationMetadata()
                .getAnnotationValuesByName(JakartaInterceptorSupport.SCHEDULED)
                .stream()
                .findFirst()
                .map(ScheduledTimer::of)
                .orElse(null);
        }
        return timer;
    }

    /**
     * A Kotlin suspending function is compiled with a continuation of its own after the arguments it declares.
     * That argument belongs to the language rather than to the invocation, so it is not one of the parameters the
     * interceptor is shown, nor one it is asked to supply.
     *
     * @return The number of declared arguments
     */
    @Override
    int declaredParameterCount() {
        int count = super.declaredParameterCount();
        return context.isSuspend() ? count - 1 : count;
    }

    @Override
    public @Nullable Object[] getParameters() {
        return readParameters();
    }

    @Override
    public void setParameters(@Nullable Object[] params) {
        writeParameters(params);
    }

    @Override
    String description() {
        return "method " + context.getDeclaringType().getName() + "." + context.getMethodName() + "(..)";
    }
}
