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

import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.interceptor.MicronautConstructorInvocationContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * The {@code InvocationContext} of an {@code @AroundConstruct} interceptor method.
 *
 * <p>The target does not exist before {@link #proceed()} has created it, which is why {@code getTarget()} is
 * {@code null} until then, exactly as the specification describes.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ConstructorInvocationContextAdapter extends AbstractInvocationContext
    implements MicronautConstructorInvocationContext {

    private final ConstructorInvocationContext<Object> context;
    private @Nullable Object target;

    ConstructorInvocationContextAdapter(ConstructorInvocationContext<Object> context,
                                        List<InterceptorReference> chain,
                                        JakartaInterceptorAdvice advice) {
        super(context, chain, advice);
        this.context = context;
    }

    @Override
    public @Nullable Object getTarget() {
        return target;
    }

    /**
     * The specification hands the interceptor a {@link Constructor}, so one is looked up. That lookup is the only
     * reflection of an {@code @AroundConstruct} interception, and it only happens when an interceptor asks for the
     * constructor.
     *
     * <p>What Micronaut constructs for a bean that also has around advice is the proxy it generated, whose
     * constructor takes a few arguments of its own after the declared ones. The bean constructor it hands a
     * constructor interceptor describes the constructor of the target class regardless, and resolves and remembers
     * that constructor itself, so the declaring type and the parameters are the ones the specification asks
     * for.</p>
     *
     * <p>The lookup is Micronaut Core's own, {@code BeanConstructor.getTargetConstructor()}, so unlike the method of
     * the platform and the instances of the bindings the constructor needs no reflection module.</p>
     *
     * @return The constructor of the target class, or {@code null} when it cannot be found
     */
    @Override
    public @Nullable Constructor<?> getConstructor() {
        return context.getConstructor().getTargetConstructor();
    }

    @Override
    public @Nullable Object[] getParameters() {
        return readParameters();
    }

    @Override
    public void setParameters(@Nullable Object[] params) {
        writeParameters(params);
    }

    /**
     * Constructing something produces no value of its own, and the specification has the {@code proceed} of an
     * {@code @AroundConstruct} method return {@code null} rather than the instance. The instance is what
     * {@link #getTarget()} returns from here on.
     *
     * <p>The constructor runs once. An interceptor may proceed a second time, to run the rest of the chain again
     * after it threw, and a construction that failed is then attempted again; one that succeeded is not, since a
     * second instance would only replace, and lose, the one already made.</p>
     *
     * @return Always {@code null}
     */
    @Override
    @Nullable Object proceedTarget() {
        if (target == null) {
            target = super.proceedTarget();
        }
        return null;
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        // a constructor invocation carries no metadata of its own: the constructor does
        return context.getConstructor().getAnnotationMetadata();
    }

    @Override
    public ConstructorInvocationContext<Object> getMicronautInvocation() {
        return context;
    }

    @Override
    String description() {
        return "constructor " + context.getConstructor().getDescription(false);
    }
}
