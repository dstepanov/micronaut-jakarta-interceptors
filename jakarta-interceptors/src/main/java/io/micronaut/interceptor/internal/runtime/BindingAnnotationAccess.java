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
import io.micronaut.core.util.CollectionUtils;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the binding annotations an {@code InvocationContext} hands out from the binding values that selected its
 * chain, and picks those of one annotation type out of them.
 *
 * <p>It holds nothing: what is built is kept by the invocation context, or by the method that is invoked again and
 * again, as before. An annotation is built only when an interceptor asks for the bindings - by the builder Micronaut
 * generated for it with {@code @RegisterAnnotations} where there is one, by the reflection module otherwise, whose
 * absence {@link ReflectionModule} reports. A builder is looked for with {@link AnnotationBuilder#find(Class,
 * ClassLoader)}, among those of Micronaut, of the class loader of the binding type, and of the application's class
 * loader.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class BindingAnnotationAccess {

    private BindingAnnotationAccess() {
    }

    /**
     * The binding of one annotation type among the binding annotations already built.
     *
     * @param built          The binding annotations
     * @param annotationType The binding annotation type
     * @param <T>            The binding annotation type
     * @return The binding, or {@code null} where there is none of that type
     */
    static <T extends Annotation> @Nullable T first(Set<Annotation> built, Class<T> annotationType) {
        for (Annotation binding : built) {
            if (binding.annotationType().equals(annotationType)) {
                return annotationType.cast(binding);
            }
        }
        return null;
    }

    /**
     * The bindings of one annotation type among the binding annotations already built.
     *
     * @param built          The binding annotations
     * @param annotationType The binding annotation type
     * @param <T>            The binding annotation type
     * @return The bindings of that type
     */
    static <T extends Annotation> Set<T> all(Set<Annotation> built, Class<T> annotationType) {
        Set<T> matching = new LinkedHashSet<>(1);
        for (Annotation binding : built) {
            if (binding.annotationType().equals(annotationType)) {
                matching.add(annotationType.cast(binding));
            }
        }
        return Collections.unmodifiableSet(matching);
    }

    /**
     * Builds the binding of one annotation type from the binding values, without building the others.
     *
     * @param values         The binding values
     * @param annotationType The binding annotation type
     * @param classLoader    The class loader of the application
     * @param <T>            The binding annotation type
     * @return The binding, or {@code null} where there is none of that type
     */
    static <T extends Annotation> @Nullable T first(List<AnnotationValue<?>> values,
                                                    Class<T> annotationType,
                                                    ClassLoader classLoader) {
        String name = annotationType.getName();
        for (AnnotationValue<?> value : values) {
            if (value.getAnnotationName().equals(name)) {
                // getInterceptorBinding returns the annotation itself, so one has to be built
                return instanceOf(annotationType, value, "getInterceptorBinding(Class)", classLoader);
            }
        }
        return null;
    }

    /**
     * Builds the bindings of one annotation type from the binding values, without building the others.
     *
     * @param values         The binding values
     * @param annotationType The binding annotation type
     * @param classLoader    The class loader of the application
     * @param <T>            The binding annotation type
     * @return The bindings of that type
     */
    static <T extends Annotation> Set<T> all(List<AnnotationValue<?>> values,
                                             Class<T> annotationType,
                                             ClassLoader classLoader) {
        String name = annotationType.getName();
        Set<T> matching = new LinkedHashSet<>(1);
        for (AnnotationValue<?> value : values) {
            if (value.getAnnotationName().equals(name)) {
                // the bindings are the annotations themselves, so they have to be built
                matching.add(instanceOf(annotationType, value, "getInterceptorBindings(Class)", classLoader));
            }
        }
        return Collections.unmodifiableSet(matching);
    }

    /**
     * Builds every binding annotation from the binding values.
     *
     * @param metadata    The metadata the bindings were read from, which finds the type of a binding by its name
     * @param values      The binding values, of which there is at least one
     * @param classLoader The class loader of the application
     * @return The binding annotations
     */
    static Set<Annotation> build(AnnotationMetadata metadata,
                                 List<AnnotationValue<?>> values,
                                 ClassLoader classLoader) {
        String accessor = "getInterceptorBindings()";
        Set<Annotation> resolved = CollectionUtils.newLinkedHashSet(values.size());
        for (AnnotationValue<?> value : values) {
            bindingType(metadata, value.getAnnotationName(), accessor)
                .ifPresent(type -> resolved.add(instanceOf(type, value, accessor, classLoader)));
        }
        return Collections.unmodifiableSet(resolved);
    }

    /**
     * The type of a binding annotation the element carries, found by its name.
     *
     * <p>A binding whose builder Micronaut generated with {@code @RegisterAnnotations} among its own builders is
     * found through the builder, which knows its type. Any other is a class found by its name, whose builder is
     * looked for by its type when its instance is built. A binding whose class is not found is not built, and
     * without the reflection module its absence is reported rather than the binding left out.</p>
     */
    private static Optional<? extends Class<? extends Annotation>> bindingType(AnnotationMetadata metadata,
                                                                               String name,
                                                                               String accessor) {
        Optional<AnnotationBuilder<?>> builder = AnnotationBuilder.find(name);
        if (builder.isPresent()) {
            return Optional.of(builder.get().annotationType());
        }
        Optional<Class<? extends Annotation>> type = metadata.getAnnotationType(name);
        if (type.isEmpty()) {
            ReflectionModule.require(accessor);
        }
        return type;
    }

    /**
     * Builds an instance of a binding annotation from the value Micronaut recorded for it.
     *
     * <p>A builder Micronaut generated for the annotation with {@code @RegisterAnnotations} builds it without the
     * reflection module, and without a proxy class, whether it is registered with Micronaut, with the binding type
     * or with the application's class loader. An annotation without one is synthesized by the reflection module.</p>
     *
     * @param value       A value of the annotation of the given type, which is what it is read as
     * @param classLoader The class loader of the application
     */
    @SuppressWarnings("unchecked")
    private static <T extends Annotation> T instanceOf(Class<T> annotationType,
                                                       AnnotationValue<?> value,
                                                       String accessor,
                                                       ClassLoader classLoader) {
        AnnotationValue<T> typed = (AnnotationValue<T>) value;
        Optional<AnnotationBuilder<T>> builder = AnnotationBuilder.find(annotationType, classLoader);
        if (builder.isPresent()) {
            return builder.get().build(typed);
        }
        ReflectionModule.require(accessor);
        return PlatformReflection.annotation(annotationType, typed);
    }
}
