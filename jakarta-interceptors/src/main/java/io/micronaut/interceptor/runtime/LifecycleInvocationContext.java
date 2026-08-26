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

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The {@code InvocationContext} of a {@code @PostConstruct} or {@code @PreDestroy} interceptor method.
 *
 * <p>The specification requires {@code getMethod()} to be {@code null} and {@code getParameters()} to fail for a
 * lifecycle callback, both of which the base class already does. What is left is the target, which exists for the
 * whole of the callback, and the description of the element for the error messages.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class LifecycleInvocationContext extends AbstractInvocationContext {

    private final MethodInvocationContext<Object, ?> context;

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
