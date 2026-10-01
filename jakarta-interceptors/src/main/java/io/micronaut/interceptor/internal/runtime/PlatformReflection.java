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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanConstructor;
import io.micronaut.inject.MethodReference;
import io.micronaut.reflection.ReflectionAnnotations;
import io.micronaut.reflection.ReflectionExecutables;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/**
 * Everything of this module that hands back an object of the reflection of the platform: the {@link Method} and
 * the {@link Constructor} of an intercepted element, and the instances of its binding annotations.
 *
 * <p>It is answered by {@code io.micronaut:micronaut-reflection}, which this module compiles against and does not
 * depend on. The class links to that module, so it initializes only where the module is on the classpath, and it is
 * loaded only when an interceptor calls one of the accessors of {@code InvocationContext} that return such an
 * object. Where the module is absent its use fails with a {@link NoClassDefFoundError}, which the accessor reports
 * as an {@link UnsupportedOperationException} that names the module. Nothing probes for the module beforehand, and
 * an application whose interceptors call none of those accessors never loads this class at all.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class PlatformReflection {

    static {
        // links this class to the reflection module: not every lookup below is answered by a class of that module,
        // and all of them are to fail alike without it
        Objects.requireNonNull(ReflectionExecutables.class);
    }

    private PlatformReflection() {
    }

    /**
     * @param method The intercepted method
     * @return The method of the platform. A lifecycle event of a class that declares no callback has none, and
     * Micronaut answers {@code null} for it
     */
    static Method method(MethodReference<?, ?> method) {
        return ReflectionExecutables.targetMethod(method);
    }

    /**
     * @param constructor The intercepted constructor
     * @return The constructor of the platform, or {@code null} when the bean type declares no such constructor
     */
    static @Nullable Constructor<?> constructor(BeanConstructor<?> constructor) {
        return constructor.getTargetConstructor();
    }

    /**
     * @param metadata The metadata that carries the annotation
     * @param name     The name of the annotation type
     * @return The annotation type, where the class is present
     */
    static Optional<Class<? extends Annotation>> annotationType(AnnotationMetadata metadata, String name) {
        return metadata.getAnnotationType(name);
    }

    /**
     * @param annotationType The annotation type
     * @param value          The value Micronaut recorded for it at compilation time
     * @param <A>            The annotation type
     * @return An instance of the annotation
     */
    static <A extends Annotation> A annotation(Class<A> annotationType, AnnotationValue<A> value) {
        return ReflectionAnnotations.synthesize(annotationType, value);
    }
}
