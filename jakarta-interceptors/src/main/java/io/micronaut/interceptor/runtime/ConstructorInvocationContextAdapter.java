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

import io.micronaut.aop.ConstructorInvocationContext;
import io.micronaut.aop.Intercepted;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanConstructor;
import io.micronaut.core.type.Argument;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Arrays;
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
final class ConstructorInvocationContextAdapter extends AbstractInvocationContext {

    private final ConstructorInvocationContext<Object> context;
    private @Nullable Object target;
    private @Nullable Constructor<?> constructor;
    private boolean constructorResolved;

    ConstructorInvocationContextAdapter(ConstructorInvocationContext<Object> context,
                                        List<InterceptorReference> chain,
                                        InterceptorInstances instances) {
        super(context, chain, instances);
        this.context = context;
    }

    @Override
    public @Nullable Object getTarget() {
        return target;
    }

    /**
     * Returns the instance the constructor produced.
     *
     * @return The instance, or {@code null} when the interceptor never proceeded
     */
    @Nullable Object constructed() {
        return target;
    }

    /**
     * The specification hands the interceptor a {@link Constructor}, so one is looked up. That lookup is the only
     * reflection of an {@code @AroundConstruct} interception, and it only happens when an interceptor asks for the
     * constructor.
     *
     * @return The constructor of the target class, or {@code null} when it cannot be found
     */
    @Override
    public @Nullable Constructor<?> getConstructor() {
        if (!constructorResolved) {
            constructorResolved = true;
            Object[] arguments = readParameters();
            constructor = resolveConstructor(context.getConstructor(), arguments == null ? 0 : arguments.length);
        }
        return constructor;
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
     * @return Always {@code null}
     */
    @Override
    @Nullable Object proceedTarget() {
        target = context.proceed();
        return null;
    }

    @Override
    AnnotationMetadata annotationMetadata() {
        // a constructor invocation carries no metadata of its own: the constructor does
        return context.getConstructor().getAnnotationMetadata();
    }

    @Override
    String description() {
        return "constructor " + context.getConstructor().getDescription(false);
    }

    /**
     * Finds the constructor of the class the specification calls the target class.
     *
     * <p>What is being constructed is the proxy Micronaut generated, whose class is a subclass of the target class
     * and whose constructor takes a few arguments of its own after the declared ones. The constructor the
     * interceptor is shown is the one of the target class itself.</p>
     *
     * @param beanConstructor The constructor of the invocation
     * @param argumentCount   How many of its arguments were declared by the target class
     * @return The constructor, or {@code null} when it cannot be found
     */
    private static @Nullable Constructor<?> resolveConstructor(BeanConstructor<?> beanConstructor, int argumentCount) {
        Class<?> declaringType = beanConstructor.getDeclaringBeanType();
        if (Intercepted.class.isAssignableFrom(declaringType)) {
            Class<?> superclass = declaringType.getSuperclass();
            if (superclass != null) {
                declaringType = superclass;
            }
        }
        Class<?>[] parameterTypes = Arrays.stream(beanConstructor.getArguments())
            .limit(argumentCount)
            .map(Argument::getType)
            .toArray(Class<?>[]::new);
        try {
            return declaringType.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
