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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;
import jakarta.interceptor.InvocationContext;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The part of {@link InvocationContext} that is the same whichever element is being intercepted.
 *
 * <p>One context drives the whole chain of interceptor classes that applies to an element: {@link #proceed()} walks
 * the chain the processor and the bean context resolved, and hands over to Micronaut only once the chain is
 * exhausted, so that whatever comes after - another Micronaut interceptor, or the intercepted element itself -
 * runs last, as the specification requires.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
abstract sealed class AbstractInvocationContext implements InvocationContext
    permits BusinessMethodInvocationContext, ConstructorInvocationContextAdapter, LifecycleInvocationContext {

    /**
     * The attribute the context data is shared under. The Micronaut interceptor chain owns one attribute map for
     * the whole chain, so the context data is visible not only to the interceptor classes of this chain but also
     * to any other advice of the same invocation.
     */
    private static final String CONTEXT_DATA = "io.micronaut.interceptor.contextData";

    private final io.micronaut.aop.InvocationContext<Object, ?> context;
    private final List<InterceptorReference> chain;
    private final InterceptorInstances instances;
    private int index;
    private @Nullable Set<Annotation> bindings;

    AbstractInvocationContext(io.micronaut.aop.InvocationContext<Object, ?> context,
                              List<InterceptorReference> chain,
                              InterceptorInstances instances) {
        this.context = context;
        this.chain = chain;
        this.instances = instances;
    }

    @Override
    public @Nullable Object getTimer() {
        // the timer service belongs to Jakarta Enterprise Beans, which has no counterpart here
        return null;
    }

    @Override
    public @Nullable Method getMethod() {
        return null;
    }

    @Override
    public @Nullable Constructor<?> getConstructor() {
        return null;
    }

    @Override
    public @Nullable Object[] getParameters() {
        throw new IllegalStateException("getParameters() is not available on " + description());
    }

    @Override
    public void setParameters(@Nullable Object[] params) {
        throw new IllegalStateException("setParameters() is not available on " + description());
    }

    @Override
    public @Nullable Object proceed() throws Exception {
        if (index < chain.size()) {
            int current = index++;
            InterceptorReference reference = chain.get(current);
            try {
                return reference.invoke(instanceOf(reference), this);
            } finally {
                // the position is where it was before, so that an interceptor which proceeds a second time - to
                // recover from what the rest of the chain threw, as the specification lets it - runs the rest of
                // the chain again rather than dropping straight through to the intercepted element
                index = current;
            }
        }
        return proceedTarget();
    }

    private Object instanceOf(InterceptorReference reference) {
        if (!reference.self()) {
            return instances.get(reference);
        }
        Object target = getTarget();
        if (target == null) {
            throw new IllegalStateException("An interceptor method declared by the intercepted class itself cannot "
                + "interpose on " + description() + ", where there is no instance to invoke it on");
        }
        return target;
    }

    /**
     * Hands the invocation over to Micronaut, once every interceptor class of the chain has proceeded.
     *
     * @return The result of the invocation
     */
    @Nullable Object proceedTarget() {
        return context.proceed();
    }

    @Override
    public Map<String, Object> getContextData() {
        MutableConvertibleValues<Object> attributes = context.getAttributes();
        // the attribute is read as it was stored: asking for it as a map would convert it into a copy, which the
        // interceptors of the chain would then no longer share
        ContextData data = contextDataOf(attributes);
        if (data == null) {
            synchronized (attributes) {
                data = contextDataOf(attributes);
                if (data == null) {
                    data = new ContextData();
                    attributes.put(CONTEXT_DATA, data);
                }
            }
        }
        return data;
    }

    private static @Nullable ContextData contextDataOf(MutableConvertibleValues<Object> attributes) {
        return attributes.getValue(CONTEXT_DATA) instanceof ContextData data ? data : null;
    }

    /**
     * The annotation metadata of the intercepted element.
     *
     * @return The metadata
     */
    AnnotationMetadata annotationMetadata() {
        return context.getAnnotationMetadata();
    }

    /**
     * The bindings are instances of the binding annotations, which the specification asks for as such. Building
     * them is the one thing the interception does that needs the reflection of the platform, and it only happens
     * when an interceptor asks for the bindings; the interception itself compares the bindings that Micronaut
     * recorded at compilation time, and never builds an annotation.
     *
     * @return The binding annotations in effect on the intercepted element
     */
    @Override
    public Set<Annotation> getInterceptorBindings() {
        Set<Annotation> resolved = bindings;
        if (resolved == null) {
            resolved = resolveBindings();
            bindings = resolved;
        }
        return resolved;
    }

    private Set<Annotation> resolveBindings() {
        AnnotationMetadata annotationMetadata = annotationMetadata();
        List<String> names = annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptorSupport.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Annotation> resolved = new LinkedHashSet<>(names.size());
        for (String name : names) {
            annotationMetadata.getAnnotationType(name)
                .map(annotationMetadata::synthesize)
                .ifPresent(resolved::add);
        }
        return Collections.unmodifiableSet(resolved);
    }

    /**
     * Describes the intercepted element for the message of an {@link IllegalStateException}.
     *
     * @return The description
     */
    abstract String description();

    /**
     * How many of the arguments the invocation carries were declared by the intercepted element.
     *
     * <p>All of them, unless the language added one of its own, which a Kotlin suspending function does.</p>
     *
     * @return The number of declared arguments
     */
    int declaredParameterCount() {
        @Nullable Object[] parameters = context.getParameterValues();
        return parameters == null ? 0 : parameters.length;
    }

    /**
     * Reads the current arguments of the intercepted element.
     *
     * <p>A copy is returned: the specification has an interceptor replace the arguments through
     * {@link #setParameters}, so writing into the array that was read must not reach the invocation. Only the
     * arguments the element declared are shown; an argument the language added is none of the interceptor's
     * business and could not be replaced meaningfully anyway.</p>
     *
     * @return The arguments
     */
    final @Nullable Object[] readParameters() {
        @Nullable Object[] parameters = context.getParameterValues();
        if (parameters == null) {
            return new Object[0];
        }
        return Arrays.copyOf(parameters, declaredParameterCount());
    }

    /**
     * Replaces the arguments the rest of the chain, and finally the intercepted element itself, is invoked with.
     *
     * <p>The arguments are written into the array the chain holds them in, which is the array it goes on to invoke
     * with, rather than one by one through the parameter map of the chain: the array is what a constructor
     * invocation is proceeded with, and the map does not reach it.</p>
     *
     * @param params The new arguments
     */
    final void writeParameters(@Nullable Object[] params) {
        @Nullable Object[] current = context.getParameterValues();
        int declared = declaredParameterCount();
        if (params == null || current == null || params.length != declared) {
            throw new IllegalArgumentException("Expected " + declared
                + " parameter(s) for " + description() + " but got " + (params == null ? 0 : params.length));
        }
        Argument<?>[] arguments = context.getArguments();
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (i < arguments.length && !isAssignable(arguments[i].getType(), value)) {
                throw new IllegalArgumentException("Parameter [" + arguments[i].getName() + "] of " + description()
                    + " is of type " + arguments[i].getType().getName() + " and cannot be set to " + value);
            }
        }
        System.arraycopy(params, 0, current, 0, params.length);
    }

    private static boolean isAssignable(Class<?> type, @Nullable Object value) {
        if (value == null) {
            return !type.isPrimitive();
        }
        if (type.isPrimitive()) {
            // an argument of a primitive type is passed boxed
            return ReflectionUtils.getWrapperType(type).isInstance(value);
        }
        return type.isInstance(value);
    }
}
