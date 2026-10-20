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

import io.micronaut.core.annotation.AnnotationBuilder;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.type.Argument;
import io.micronaut.interceptor.MicronautInvocationContext;
import io.micronaut.interceptor.spi.InterceptorBindings;
import io.micronaut.reflection.ReflectionExecutables;
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
import java.util.Objects;
import java.util.Optional;
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

    private final io.micronaut.aop.InvocationContext<Object, ?> context;
    private final List<InterceptorReference> chain;
    private final JakartaInterceptorAdvice advice;
    private int index;
    private @Nullable List<AnnotationValue<?>> bindingValues;
    private @Nullable Set<Annotation> bindings;

    AbstractInvocationContext(io.micronaut.aop.InvocationContext<Object, ?> context,
                              List<InterceptorReference> chain,
                              JakartaInterceptorAdvice advice) {
        this.context = context;
        this.chain = chain;
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
            return advice.interceptorInstance(reference);
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
        advice.createInterceptorInstances(chain);
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
            // the attributes cannot put a value only where there is none, so the map is created under their lock
            // and looked for again once it is held. See the guide, "Waiting on Micronaut core"
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
     * The annotation metadata of the intercepted element, as Micronaut hands it to the advice.
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
     * The bindings are instances of the binding annotations, which the specification asks for as such, built from
     * {@link #getInterceptorBindingValues()}, the bindings that selected the chain. An annotation is built only when
     * an interceptor asks for the bindings - by the builder Micronaut generated for it with
     * {@code @RegisterAnnotations} where there is one, by the reflection module otherwise; the interception itself
     * compares the bindings the processor recorded, and never builds an annotation.
     *
     * @return The binding annotations that selected the chain of the intercepted element
     */
    @Override
    public Set<Annotation> getInterceptorBindings() {
        Set<Annotation> resolved = bindings;
        if (resolved == null) {
            resolved = elementBindings();
            bindings = resolved;
        }
        return resolved;
    }

    /**
     * The binding annotations of the intercepted element, built from its metadata. An invocation of an element that
     * is invoked again and again answers what the element remembers instead.
     *
     * @return The binding annotations
     */
    Set<Annotation> elementBindings() {
        return resolveBindings();
    }

    /**
     * The binding values of the intercepted element, read from its metadata. An invocation of an element that is
     * invoked again and again answers what the element remembers instead.
     *
     * @return The binding values
     */
    List<AnnotationValue<?>> elementBindingValues() {
        return readBindingValues();
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
        String name = annotationType.getName();
        for (AnnotationValue<?> value : getInterceptorBindingValues()) {
            if (value.getAnnotationName().equals(name)) {
                // getInterceptorBinding returns the annotation itself, so one has to be built
                return instanceOf(annotationType, value, "getInterceptorBinding(Class)");
            }
        }
        return null;
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
        String name = annotationType.getName();
        Set<T> matching = new LinkedHashSet<>(1);
        for (AnnotationValue<?> value : getInterceptorBindingValues()) {
            if (value.getAnnotationName().equals(name)) {
                // the bindings are the annotations themselves, so they have to be built
                matching.add(instanceOf(annotationType, value, "getInterceptorBindings(Class)"));
            }
        }
        return Collections.unmodifiableSet(matching);
    }

    /**
     * The bindings that selected the interceptors of the element, as values, built into nothing.
     *
     * <p>Which bindings they are, and how many occurrences of a repeatable one, is what the processor recorded on the
     * element, which is what resolving the chain read: a binding only the method an element overrides declares, or
     * that only the factory producing a bean declares, is not one of them, although the metadata of the element
     * holds it, and occurrences that differ only in a member that does not bind are one binding. What was recorded
     * holds only the members that bind, so each binding is reported as the declaration it was reduced from, which
     * holds the members that do not bind as well: the nearest declaration of it that is the same binding, the
     * element's before its class's.</p>
     *
     * @return The values of the bindings
     */
    @Override
    public List<AnnotationValue<?>> getInterceptorBindingValues() {
        List<AnnotationValue<?>> values = bindingValues;
        if (values == null) {
            values = elementBindingValues();
            bindingValues = values;
        }
        return values;
    }

    final List<AnnotationValue<?>> readBindingValues() {
        return InterceptorBindings.declarations(bindingMetadata());
    }

    final Set<Annotation> resolveBindings() {
        List<AnnotationValue<?>> values = getInterceptorBindingValues();
        if (values.isEmpty()) {
            return Collections.emptySet();
        }
        AnnotationMetadata annotationMetadata = bindingMetadata();
        String accessor = "getInterceptorBindings()";
        Set<Annotation> resolved = new LinkedHashSet<>(values.size());
        for (AnnotationValue<?> value : values) {
            bindingType(annotationMetadata, value.getAnnotationName(), accessor)
                .ifPresent(type -> resolved.add(instanceOf(type, value, accessor)));
        }
        return Collections.unmodifiableSet(resolved);
    }

    /**
     * The type of a binding annotation the element carries, found by its name.
     *
     * <p>A binding whose builder Micronaut generated with {@code @RegisterAnnotations} is found through the builder,
     * which knows its type. Any other is a class found by its name, whose instances are left to the reflection
     * module to build.</p>
     */
    private static Optional<? extends Class<? extends Annotation>> bindingType(AnnotationMetadata metadata,
                                                                              String name,
                                                                              String accessor) {
        Optional<AnnotationBuilder<?>> builder = AnnotationBuilder.find(name);
        if (builder.isPresent()) {
            return Optional.of(builder.get().annotationType());
        }
        // finding the class asks nothing of the reflection module, but building its instance does, so its absence
        // is reported here, before the type is looked for
        requireReflectionModule(accessor);
        return metadata.getAnnotationType(name);
    }

    /**
     * Builds an instance of a binding annotation from the value Micronaut recorded for it.
     *
     * <p>A builder Micronaut generated for the annotation with {@code @RegisterAnnotations} builds it without the
     * reflection module, and without a proxy class. An annotation without one is synthesized by the reflection
     * module.</p>
     *
     * @param value A value of the annotation of the given type, which is what it is read as
     */
    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T instanceOf(Class<T> annotationType,
                                                       AnnotationValue<?> value,
                                                       String accessor) {
        AnnotationValue<T> typed = (AnnotationValue<T>) value;
        Optional<AnnotationBuilder<T>> builder = AnnotationBuilder.find(annotationType);
        if (builder.isPresent()) {
            return builder.get().build(typed);
        }
        requireReflectionModule(accessor);
        return PlatformReflection.annotation(annotationType, typed);
    }

    /**
     * Makes sure the reflection module is on the classpath before {@link PlatformReflection} is used to answer an
     * accessor.
     *
     * <p>{@link PlatformReflection} links to that module and fails to load without it. Rather than recognising that
     * failure among the others an accessor may run into, the module is looked for once, by {@link ReflectionModule},
     * and an accessor that needs it asks first. A class of the application that is missing - one a binding annotation
     * refers to, for instance - therefore fails as it is, naming that class, and is never taken for the module.</p>
     *
     * @param accessor The accessor of {@code InvocationContext} that was called
     * @throws UnsupportedOperationException naming the accessor and the dependency to add, where the module is absent
     */
    static void requireReflectionModule(String accessor) {
        NoClassDefFoundError missing = ReflectionModule.MISSING;
        if (missing != null) {
            throw reflectionUnavailable(accessor, missing);
        }
    }

    /**
     * The failure of an accessor that returns an object of the reflection of the platform, in an application
     * without the module that answers it.
     *
     * @param accessor The accessor of {@code InvocationContext} that was called
     * @param cause    What looking for the module failed with
     * @return The exception to throw
     */
    static UnsupportedOperationException reflectionUnavailable(String accessor, NoClassDefFoundError cause) {
        return new UnsupportedOperationException("InvocationContext." + accessor + " returns an object of the Java"
            + " reflection API, which needs io.micronaut:micronaut-reflection on the classpath (or"
            + " io.micronaut.cdi:micronaut-cdi-reflection, which brings it, when the interceptors are used through"
            + " Micronaut CDI). Add that dependency, or read the interception through MicronautInvocationContext,"
            + " which reflects on nothing.", cause);
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
     * <p>A copy is returned. The specification defines replacement through {@link #setParameters}, but does not
     * specify array identity or aliasing. This implementation owns the argument array and requires an explicit
     * setter call to replace its values. Only the arguments the element declared are shown; an argument the
     * language added is not exposed.</p>
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
        // Validate the same snapshot that is installed. The caller still owns params and may change it;
        // copying after validation would allow a changed, unchecked value to enter the invocation.
        Object[] replacement = params.clone();
        Argument<?>[] arguments = context.getArguments();
        for (int i = 0; i < replacement.length; i++) {
            Object value = replacement[i];
            if (i < arguments.length && !isAssignable(arguments[i], value)) {
                throw new IllegalArgumentException("Parameter [" + arguments[i].getName() + "] of " + description()
                    + " is of type " + arguments[i].getType().getName() + " and cannot be set to " + value);
            }
        }
        System.arraycopy(replacement, 0, current, 0, replacement.length);
    }

    private static boolean isAssignable(Argument<?> argument, @Nullable Object value) {
        if (value == null) {
            return !argument.isPrimitive();
        }
        // an argument of a primitive type is passed boxed, and its wrapper type is the type itself otherwise
        return argument.getWrapperType().isInstance(value);
    }

    /**
     * Whether the reflection module is on the classpath, found out the first time an accessor that needs it is
     * called - a class is initialized on its first use, so an application whose interceptors call none of those
     * accessors never looks for the module at all.
     */
    static final class ReflectionModule {

        /**
         * What loading a class of the module failed with, or {@code null} where the module is there. It is the
         * cause of every failure that reports the module missing, which is the same for each of them.
         */
        @SuppressWarnings("StaticAssignmentOfThrowable")
        static final @Nullable NoClassDefFoundError MISSING = find();

        private ReflectionModule() {
        }

        private static @Nullable NoClassDefFoundError find() {
            try {
                // a class literal is resolved, and so loaded, where it is evaluated
                Objects.requireNonNull(ReflectionExecutables.class);
                return null;
            } catch (NoClassDefFoundError e) {
                return e;
            }
        }
    }
}
