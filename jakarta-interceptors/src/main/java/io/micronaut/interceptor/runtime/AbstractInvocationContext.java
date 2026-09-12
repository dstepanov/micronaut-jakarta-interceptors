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
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.interceptor.MicronautInvocationContext;
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
abstract sealed class AbstractInvocationContext implements MicronautInvocationContext
    permits BusinessMethodInvocationContext, ConstructorInvocationContextAdapter, LifecycleInvocationContext {

    /**
     * The attribute the context data is shared under. The Micronaut interceptor chain owns one attribute map for
     * the whole chain, so the context data is visible not only to the interceptor classes of this chain but also
     * to any other advice of the same invocation.
     */
    private static final String CONTEXT_DATA = "io.micronaut.interceptor.contextData";

    private static final Annotation[] EMPTY_BINDINGS = new Annotation[0];

    private final io.micronaut.aop.InvocationContext<Object, ?> context;
    private final List<InterceptorReference> chain;
    private final InterceptorInstances instances;
    private final Interceptor<?, ?> advice;
    private int index;
    private @Nullable Set<Annotation> bindings;

    AbstractInvocationContext(io.micronaut.aop.InvocationContext<Object, ?> context,
                              List<InterceptorReference> chain,
                              InterceptorInstances instances,
                              Interceptor<?, ?> advice) {
        this.context = context;
        this.chain = chain;
        this.instances = instances;
        this.advice = advice;
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
     * Creates the instance of every interceptor class of the chain that has none yet, rather than each as the chain
     * reaches it.
     */
    final void createInterceptorInstances() {
        instances.createAll(chain);
    }

    /**
     * Destroys the interceptor instances of the object being intercepted, which has failed to be created.
     *
     * <p>Section 2.3 destroys the interceptor instances of an object that fails to be created, as it does those of an
     * object that is removed. Micronaut has nothing to destroy them with in that case: the advice holding them is
     * only destroyed together with a bean that exists.</p>
     */
    final void discardInterceptorInstances() {
        instances.destroy();
    }

    /**
     * Hands the invocation over to Micronaut, once every interceptor class of the chain has proceeded.
     *
     * <p>What it hands over to is whatever Micronaut ordered after the advice, which is not always the intercepted
     * element: another Micronaut interceptor may come first. The Micronaut chain is proceeded from the position of
     * the advice rather than from wherever it stands now. An interceptor that proceeds a second time, to recover
     * from what the rest of the chain threw, finds the Micronaut chain already moved past everything it ran the
     * first time, and proceeding it from there would run the intercepted element alone, leaving out the Micronaut
     * interceptors between the two.</p>
     *
     * <p>Micronaut finds the position of the advice by its identity, resuming after the first interceptor of the
     * invocation that is the same object. That is the position of this advice because the advice is a prototype
     * created for the one object it intercepts, and Micronaut builds the interceptors of an invocation from the
     * interceptor registrations of the proxy, one interceptor apiece: however much a bean declares, it is bound to
     * one advice and that advice is in the array once. Were it there twice, resuming from its second occurrence
     * would return to that occurrence rather than to what comes after it. {@code AdviceIdentityTest} holds the
     * binding to one advice.</p>
     *
     * @return The result of the invocation
     */
    @Nullable Object proceedTarget() {
        return context.proceed(advice);
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
     * The annotation metadata of the intercepted element, which is what the chain was resolved by.
     *
     * @return The metadata
     */
    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return context.getAnnotationMetadata();
    }

    /**
     * The metadata the bindings of the intercepted element are read from, which is the metadata its interception was
     * resolved from.
     *
     * <p>Micronaut hands the advice a wrapper around the metadata of an element whose annotations contain an
     * evaluated expression anywhere, so that the expression can read the invocation. Asked for an annotation, that
     * wrapper merges the members the element declares with the members its class declares, member by member - which
     * for a binding resurrects the value the element replaced: a method declaring {@code @Zone} where its class
     * declares {@code @Zone("a")} is bound by the default of the member, and the merged annotation says {@code "a"}.
     * The wrapper is therefore taken off, leaving the metadata Micronaut compiled, where a declaration of the element
     * replaces the one of its class whole, as resolving the chain read it. Core takes it off the same way before it
     * resolves which interceptors are bound to an element.</p>
     *
     * <p>A binding whose member is itself an expression is read unevaluated, as the comparison that bound the
     * interceptor read it.</p>
     */
    private AnnotationMetadata bindingMetadata() {
        return getAnnotationMetadata().getTargetAnnotationMetadata();
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

    /**
     * The binding of one annotation type, which the specification added in 2.2 alongside the whole set.
     *
     * <p>Answered without building the rest. The inherited default reads {@link #getInterceptorBindings()} and
     * filters it, which for this implementation means synthesizing every binding of the element - a dynamic proxy
     * class apiece, kept for as long as the class loader - to hand back one of them. Where the whole set has
     * already been built it is read, since the annotation asked for is in it.</p>
     *
     * @param annotationType The binding annotation type
     * @param <T>            The binding annotation type
     * @return The binding, or {@code null} where the element does not carry one of that type
     */
    @Override
    public <T extends Annotation> @Nullable T getInterceptorBinding(Class<T> annotationType) {
        Set<Annotation> resolved = bindings;
        if (resolved != null) {
            for (Annotation binding : resolved) {
                if (binding.annotationType().equals(annotationType)) {
                    return annotationType.cast(binding);
                }
            }
            return null;
        }
        if (!isBinding(annotationType)) {
            return null;
        }
        // getInterceptorBinding returns the annotation itself, so one has to be built
        @SuppressWarnings("NoReflection")
        T binding = bindingMetadata().synthesize(annotationType);
        return binding;
    }

    /**
     * The bindings of one annotation type, of which a repeatable binding may leave several on an element.
     *
     * <p>Answered without building the rest, as {@link #getInterceptorBinding(Class)} is.</p>
     *
     * @param annotationType The binding annotation type
     * @param <T>            The binding annotation type
     * @return The bindings of that type, empty where the element carries none
     */
    @Override
    public <T extends Annotation> Set<T> getInterceptorBindings(Class<T> annotationType) {
        Set<Annotation> resolved = bindings;
        if (resolved != null) {
            Set<T> matching = new LinkedHashSet<>(1);
            for (Annotation binding : resolved) {
                if (binding.annotationType().equals(annotationType)) {
                    matching.add(annotationType.cast(binding));
                }
            }
            return Collections.unmodifiableSet(matching);
        }
        if (!isBinding(annotationType)) {
            return Collections.emptySet();
        }
        Set<T> matching = new LinkedHashSet<>(1);
        for (Annotation binding : synthesizeBindings(annotationType)) {
            matching.add(annotationType.cast(binding));
        }
        return Collections.unmodifiableSet(matching);
    }

    /**
     * The bindings of one annotation type carried by the element.
     *
     * <p>A repeatable binding declared more than once is that many bindings, and every value of it is read.
     * Anything else is one binding however many declarations it has: a binding a method declares replaces the one
     * its class declares, and reading the values of the metadata hierarchy would answer with both, resurrecting
     * the declaration the method overrode.</p>
     *
     * <p>Whether the binding repeats is read from the metadata by name rather than from the annotation type, which
     * would ask the platform for the annotations of an annotation and inflate them into the reflection data the
     * virtual machine keeps for it. Micronaut recorded the container of a repeatable annotation as the application
     * was compiled, so the metadata answers it having read nothing.</p>
     */
    private Annotation[] synthesizeBindings(Class<? extends Annotation> annotationType) {
        AnnotationMetadata annotationMetadata = bindingMetadata();
        if (annotationMetadata.findRepeatableAnnotation(annotationType.getName()).isPresent()) {
            // the bindings are the annotations themselves, so they have to be built
            @SuppressWarnings("NoReflection")
            Annotation[] repeated = annotationMetadata.synthesizeAnnotationsByType(annotationType);
            return repeated;
        }
        // as above, for a binding that does not repeat
        @SuppressWarnings("NoReflection")
        Annotation single = annotationMetadata.synthesize(annotationType);
        return single == null ? EMPTY_BINDINGS : new Annotation[]{single};
    }

    /**
     * Whether the element carries the given annotation as an interceptor binding, rather than as an annotation
     * that simply happens to be there. Read from the metadata, so nothing is built to answer it.
     */
    private boolean isBinding(Class<? extends Annotation> annotationType) {
        return bindingMetadata()
            .getAnnotationNamesByStereotype(JakartaInterceptorSupport.INTERCEPTOR_BINDING)
            .contains(annotationType.getName());
    }

    private Set<Annotation> resolveBindings() {
        AnnotationMetadata annotationMetadata = bindingMetadata();
        List<String> names = annotationMetadata.getAnnotationNamesByStereotype(JakartaInterceptorSupport.INTERCEPTOR_BINDING);
        if (names.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Annotation> resolved = new LinkedHashSet<>(names.size());
        for (String name : names) {
            annotationMetadata.getAnnotationType(name)
                .ifPresent(type -> Collections.addAll(resolved, synthesizeBindings(type)));
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
